import { defineConfig } from "tsdown";
import { fileURLToPath, URL } from "url";

export default defineConfig({
  entry: ["./src/index.ts"],
  format: ["esm", "iife"],
  external: ["vue", "vue-router"],
  platform: "browser",
  globalName: "HaloConsoleShared",
  tsconfig: "./tsconfig.app.json",
  alias: {
    "@": fileURLToPath(new URL("./src", import.meta.url)),
  },
  minify: true,
  exports: true,
  dts: {
    tsgo: false, // 禁用 tsgo 以避免 Linux 平台兼容性问题
  },
});
