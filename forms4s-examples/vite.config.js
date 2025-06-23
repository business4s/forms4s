import { defineConfig } from "vite";
import scalaJSPlugin from "@scala-js/vite-plugin-scalajs";
import { resolve } from 'path'

export default defineConfig({
    plugins: [
        scalaJSPlugin({
            cwd: '../',
            projectID: 'forms4s-examples',
        }),
    ],
});