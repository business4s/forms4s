import {TyrianApp} from 'scalajs:main.js'
import "bulma/css/bulma.min.css";

// TODO should we move it to scalajs?
function findMountPoint() {
    // If script is in shadow DOM, look relative to it
    const currentScript = document.currentScript;
    if (currentScript && currentScript.parentNode?.shadowRoot) {
        return currentScript.parentNode.shadowRoot.getElementById("forms4s-demo");
    }

    // Fallback for normal rendering
    return document.getElementById("forms4s-demo");
}

const mountPoint = findMountPoint();

if (!mountPoint) {
    throw new Error("Missing mount point #forms4s-demo");
}

window.TyrianApp = TyrianApp;
TyrianApp.launch(mountPoint)