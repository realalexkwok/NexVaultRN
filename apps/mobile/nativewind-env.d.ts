/// <reference types="nativewind/types" />

// SDK 57 的 expo CLI 会把 expo-env.d.ts 加入 .gitignore（不随库），而 expo/types 的 `*.css`
// 模块声明随之不入库 → CI 裸检出下 tsc 无法解析 global.css 副作用导入。
// 这里补上同名声明，与 expo/types 保持一致（expo/types/global.d.ts:29 `declare module '*.css'`）。
declare module '*.css';

// T2.1a: expo-font assets (Inter TTFs in assets/fonts) need a module declaration;
// expo/types declares images and css but not font files.
declare module '*.ttf';
