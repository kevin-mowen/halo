import { defineConfig } from "tsdown";

export default defineConfig({
  entry: ["./entry/index.ts"],
  format: ["esm", "iife"],
  external: ["axios"],
  noExternal: ["qs"],
  outputOptions: {
    globals: {
      axios: "axios",
    },
  },
  platform: "browser",
  globalName: "HaloApiClient",
  tsconfig: "./tsconfig.json",
  minify: true,
  exports: true,
  dts: {
    tsgo: false, // 禁用 tsgo 以避免 Linux 平台兼容性问题
  },
});
