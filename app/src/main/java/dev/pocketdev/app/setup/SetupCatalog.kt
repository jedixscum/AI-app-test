package dev.pocketdev.app.setup

import dev.pocketdev.app.core.SetupStep

object SetupCatalog {
    val steps: List<SetupStep> = listOf(
        SetupStep(
            id = "base",
            title = "Base tools",
            description = "Updates Termux packages, then installs Git, Node.js (LTS), Python, curl, OpenSSL tools and proot-distro.",
            installAsset = "setup/install_base.sh",
            checkAsset = "setup/check_base.sh",
            sizeHint = "Estimate: several hundred MB",
        ),
        SetupStep(
            id = "ubuntu",
            title = "Ubuntu (proot-distro)",
            description = "Installs an Ubuntu root filesystem you can log into from Termux. No root access needed.",
            installAsset = "setup/install_ubuntu.sh",
            checkAsset = "setup/check_ubuntu.sh",
            sizeHint = "Estimate: a few hundred MB",
        ),
        SetupStep(
            id = "codeserver",
            title = "code-server",
            description = "Installs the browser-based VS Code editor from the Termux User Repository.",
            installAsset = "setup/install_codeserver.sh",
            checkAsset = "setup/check_codeserver.sh",
            sizeHint = "Estimate: a few hundred MB",
        ),
        SetupStep(
            id = "csconfig",
            title = "Secure editor config",
            description = "Binds code-server to 127.0.0.1 only and generates a password. Nothing is exposed to the network.",
            installAsset = "setup/install_csconfig.sh",
            checkAsset = "setup/check_csconfig.sh",
            sizeHint = "Tiny",
        ),
    )

    fun byId(id: String): SetupStep? = steps.firstOrNull { it.id == id }
}
