import {defineConfig} from "vite";
import scalaJSPlugin from "@scala-js/vite-plugin-scalajs";
import path from 'path';

export default defineConfig({
    base: './',
    build: {
        lib: {
            entry: 'src/frontend/main.js',
            formats: ['es'],
            fileName: 'index'
        },
    },
    plugins: [
        scalaJSPlugin({
            cwd: '../',
            projectID: 'forms4s-examples',
        }),
    ],
});