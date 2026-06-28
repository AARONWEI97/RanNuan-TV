/**
 * Tauri NSIS 脚本补丁 - 修复快捷方式图标问题
 * 在 tauri.conf.json 的 build.beforeBuildCommand 里调用:
 * "node src-tauri/nsis-patch.js"
 */
const fs = require('fs');
const path = require('path');

const nsisPath = path.join(__dirname, 'target', 'release', 'nsis', 'x64', 'installer.nsi');

if (!fs.existsSync(nsisPath)) {
  console.log('[NSIS Patch] installer.nsi not found, skipping');
  process.exit(0);
}

let content = fs.readFileSync(nsisPath, 'utf-8');

// 0. 修改输出文件名
content = content.replace(
  '!define OUTFILE "nsis-output.exe"',
  '!define OUTFILE "..\\..\\bundle\\nsis\\RanNuan TV_${VERSION}_x64-setup.exe"'
);
console.log('[NSIS Patch] Fixed OutFile');

// 0b. 修复 SIDEBARIMAGE/HEADERIMAGE 被错误填成 icon.ico（必须是 .bmp）
content = content.replace(
  /!define SIDEBARIMAGE ".+?"/g,
  '!define SIDEBARIMAGE ""'
);
content = content.replace(
  /!define HEADERIMAGE ".+?"/g,
  '!define HEADERIMAGE ""'
);
content = content.replace(
  /!define UNINSTALLERHEADERIMAGE ".+?"/g,
  '!define UNINSTALLERHEADERIMAGE ""'
);
console.log('[NSIS Patch] Fixed SIDEBARIMAGE/HEADERIMAGE');

// 1. 添加 app_icon.ico 复制
if (!content.includes('app_icon.ico')) {
  content = content.replace(
    '; Copy main executable\n  File "${MAINBINARYSRCPATH}"',
    '; Copy main executable\n  File "${MAINBINARYSRCPATH}"\n\n  ; Copy app icon for shortcuts\n  File "/oname=app_icon.ico" "${INSTALLERICON}"'
  );
  console.log('[NSIS Patch] Added app_icon.ico copy');
}

// 2. 修改快捷方式指向 app_icon.ico
const shortcutPattern = /CreateShortcut "([^"]+\.lnk)" "\$INSTDIR\\\$\{MAINBINARYNAME\}\.exe"/g;
content = content.replace(shortcutPattern, (match, p1) => {
  return `CreateShortcut "${p1}" "$INSTDIR\${MAINBINARYNAME}.exe" "" "$INSTDIR\app_icon.ico" 0`;
});

// 2b. 添加 node_modules 复制
if (!content.includes('Copy node_modules for backend')) {
  const releaseServerDir = path.join(__dirname, 'target', 'release', 'server');
  const deps = `  ; Copy node_modules for backend (完整依赖已在 target/release/server/node_modules 中)\n  SetOutPath "$INSTDIR\\server"\n  File /r /x ".cache" /x ".pnpm" "${releaseServerDir}\\node_modules"`;
  content = content.replace('; Copy external binaries', deps + '\n\n  ; Copy external binaries');
  // 卸载时清理 node_modules
  content = content.replace(
    /Delete "\\$INSTDIR\\server\\server\.js"/,
    `Delete "$INSTDIR\\server\\server.js"\n\n  ; Delete node_modules\n  RMDir /r /REBOOTOK "$INSTDIR\\server\\node_modules"`
  );
  console.log('[NSIS Patch] Added node_modules copy');
}

// 3. 卸载时清理
if (!content.includes('Delete "$INSTDIR\app_icon.ico"')) {
  content = content.replace(
    'Delete "$INSTDIR\\${MAINBINARYNAME}.exe"',
    'Delete "$INSTDIR\\${MAINBINARYNAME}.exe"\n\n  ; Delete icon\n  Delete "$INSTDIR\\app_icon.ico"'
  );
  console.log('[NSIS Patch] Added app_icon.ico delete');
}

fs.writeFileSync(nsisPath, content, 'utf-8');
console.log('[NSIS Patch] Done');
