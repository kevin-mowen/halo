import { defineConfig } from "tsdown";

export default defineConfig({
  entry: ["./src/index.ts"],
  format: ["esm"],
  dts: {
    tsgo: false, // 禁用 tsgo 以避免 Linux 平台兼容性问题
  },
  exports: true,
});
