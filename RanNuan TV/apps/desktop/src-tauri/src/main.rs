#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

use std::fs::{self, File, OpenOptions};
use std::io::Write;
use std::path::{Path, PathBuf};
use std::process::{Command, Stdio};
use std::sync::Mutex;
use std::time::Duration;
use tauri::{
    menu::{MenuBuilder, MenuItemBuilder},
    tray::{MouseButton, MouseButtonState, TrayIconBuilder, TrayIconEvent},
    Manager,
};

#[cfg(target_os = "windows")]
use std::os::windows::process::CommandExt;

#[cfg(target_os = "windows")]
const CREATE_NO_WINDOW: u32 = 0x08000000;

/// 存储 node 后端 PID，退出时按 PID 杀进程
struct ServerPid(Mutex<Option<u32>>);

fn stop_server(app: &tauri::AppHandle) {
    if let Some(pid_state) = app.try_state::<ServerPid>() {
        if let Some(pid) = *pid_state.0.lock().unwrap() {
            let mut cmd = Command::new("taskkill");
            cmd.args(["/F", "/PID", &pid.to_string()]);
            #[cfg(target_os = "windows")]
            cmd.creation_flags(CREATE_NO_WINDOW);
            let _ = cmd.output();
        }
    }
}

#[tauri::command]
fn exit_app(app: tauri::AppHandle) {
    stop_server(&app);
    app.exit(0);
}

fn strip_verbatim(path: &std::path::Path) -> PathBuf {
    let s = path.to_string_lossy();
    let stripped = s.strip_prefix("\\\\?\\").unwrap_or(&s);
    PathBuf::from(stripped)
}

fn open_backend_log(app: &tauri::AppHandle) -> Option<(PathBuf, File)> {
    let log_dir = app.path().app_log_dir().ok()?;
    fs::create_dir_all(&log_dir).ok()?;
    let log_path = log_dir.join("backend.log");
    let file = OpenOptions::new()
        .create(true)
        .append(true)
        .open(&log_path)
        .ok()?;
    Some((log_path, file))
}

fn append_backend_log(app: &tauri::AppHandle, message: &str) {
    if let Some((_, mut file)) = open_backend_log(app) {
        let _ = writeln!(file, "{}", message);
    }
}

fn find_node_executable(server_dir: &Path) -> PathBuf {
    let runtime_name = if cfg!(target_os = "windows") {
        "node.exe"
    } else {
        "node"
    };
    let bundled = server_dir.join("node-runtime").join(runtime_name);
    if bundled.exists() {
        return strip_verbatim(&bundled);
    }
    PathBuf::from("node")
}

fn find_server_js(app: &tauri::App) -> Option<PathBuf> {
    // 1. Tauri 资源目录（生产打包）
    if let Ok(resource_dir) = app.path().resource_dir() {
        let resource_server = resource_dir.join("server").join("server.js");
        if resource_server.exists() {
            println!("[Tauri] 生产路径: {:?}", resource_server);
            return Some(resource_server);
        }
    }
    // 2. 开发路径（CARGO_MANIFEST_DIR 相对）
    let dev_server = PathBuf::from(env!("CARGO_MANIFEST_DIR")).join("../../server/server.js");
    if dev_server.exists() {
        println!("[Tauri] 开发路径: {:?}", dev_server);
        return Some(dev_server);
    }
    // 3. EXE 自身目录（通用安装路径，适配手动 NSIS 部署）
    if let Ok(exe_path) = std::env::current_exe() {
        if let Some(exe_dir) = exe_path.parent() {
            let exe_server = exe_dir.join("server").join("server.js");
            if exe_server.exists() {
                println!("[Tauri] EXE路径: {:?}", exe_server);
                return Some(exe_server);
            }
        }
    }
    // 4. CWD 降级
    let cwd_server = std::env::current_dir()
        .ok()?
        .join("server")
        .join("server.js");
    if cwd_server.exists() {
        println!("[Tauri] CWD路径: {:?}", cwd_server);
        return Some(cwd_server);
    }
    eprintln!("[Tauri] 找不到 server.js!");
    None
}

fn main() {
    tauri::Builder::default()
        .plugin(tauri_plugin_shell::init())
        .setup(|app| {
            // ====== 系统托盘 ======
            let toggle = MenuItemBuilder::with_id("toggle", "显示/隐藏").build(app)?;
            let quit = MenuItemBuilder::with_id("quit", "退出").build(app)?;
            let menu = MenuBuilder::new(app).item(&toggle).item(&quit).build()?;

            let _tray = TrayIconBuilder::new()
                .icon(app.default_window_icon().unwrap().clone())
                .menu(&menu)
                .tooltip("冉暖TV")
                .on_menu_event(|app, event| match event.id().as_ref() {
                    "toggle" => {
                        if let Some(window) = app.get_webview_window("main") {
                            if window.is_visible().unwrap_or(false) {
                                let _ = window.hide();
                            } else {
                                let _ = window.show();
                                let _ = window.set_focus();
                            }
                        }
                    }
                    "quit" => {
                        stop_server(app);
                        app.exit(0);
                    }
                    _ => {}
                })
                .on_tray_icon_event(|tray, event| {
                    if let TrayIconEvent::Click {
                        button: MouseButton::Left,
                        button_state: MouseButtonState::Up,
                        ..
                    } = event
                    {
                        let app = tray.app_handle();
                        if let Some(window) = app.get_webview_window("main") {
                            let _ = window.show();
                            let _ = window.set_focus();
                        }
                    }
                })
                .build(app)?;

            // ====== 启动后端 ======
            let app_handle = app.handle().clone();
            let mut backend_pid = None;
            if let Some(server_js) = find_server_js(app) {
                let server_js = strip_verbatim(&server_js);
                let server_dir = strip_verbatim(server_js.parent().unwrap());
                let node_executable = find_node_executable(&server_dir);
                let startup_message = format!(
                    "[Tauri] Starting backend: node={:?}, script={:?}",
                    node_executable, server_js
                );
                println!("{}", startup_message);
                append_backend_log(&app_handle, &startup_message);

                let mut cmd = Command::new(&node_executable);
                cmd.arg(&server_js)
                    .current_dir(&server_dir);
                let log_path = if let Some((path, stdout)) = open_backend_log(&app_handle) {
                    match stdout.try_clone() {
                        Ok(stderr) => {
                            cmd.stdout(Stdio::from(stdout)).stderr(Stdio::from(stderr));
                        }
                        Err(_) => {
                            cmd.stdout(Stdio::null()).stderr(Stdio::null());
                        }
                    }
                    Some(path)
                } else {
                    cmd.stdout(Stdio::null()).stderr(Stdio::null());
                    None
                };
                #[cfg(target_os = "windows")]
                cmd.creation_flags(CREATE_NO_WINDOW);

                match cmd.spawn() {
                    Ok(mut child) => {
                        let pid = child.id();
                        std::thread::sleep(Duration::from_millis(400));
                        match child.try_wait() {
                            Ok(Some(status)) => {
                                let message = format!(
                                    "[Tauri] Backend exited during startup: {}. Log: {:?}",
                                    status, log_path
                                );
                                eprintln!("{}", message);
                                append_backend_log(&app_handle, &message);
                            }
                            Ok(None) => {
                                let message = format!("[Tauri] Backend PID {} is running", pid);
                                println!("{}", message);
                                append_backend_log(&app_handle, &message);
                                backend_pid = Some(pid);
                            }
                            Err(error) => {
                                let message = format!("[Tauri] Failed to inspect backend: {}", error);
                                eprintln!("{}", message);
                                append_backend_log(&app_handle, &message);
                            }
                        }
                    }
                    Err(error) => {
                        let message = format!(
                            "[Tauri] Backend spawn failed: {}. node={:?}",
                            error, node_executable
                        );
                        eprintln!("{}", message);
                        append_backend_log(&app_handle, &message);
                    }
                }
            } else {
                append_backend_log(&app_handle, "[Tauri] server.js was not found");
            }
            app.manage(ServerPid(Mutex::new(backend_pid)));
            Ok(())
        })
        .on_window_event(|window, event| {
            if let tauri::WindowEvent::CloseRequested { api, .. } = event {
                // 关闭按钮 → 隐藏到托盘而不是退出
                let _ = window.hide();
                api.prevent_close();
            }
        })
        .invoke_handler(tauri::generate_handler![exit_app])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
