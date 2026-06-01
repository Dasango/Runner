// Dispara un GitHub Actions workflow desde Runner.
// fetch está disponible (HTTP nativo de Android, no Node.js).

const GITHUB_USER = "TU_USUARIO";
const GITHUB_REPO = "TU_REPO";
const WORKFLOW_FILE = "TU_WORKFLOW.yml";
const GITHUB_TOKEN = "ghp_TU_TOKEN";

async function dispararWorkflow() {
    const url = `https://api.github.com/repos/${GITHUB_USER}/${GITHUB_REPO}/actions/workflows/${WORKFLOW_FILE}/dispatches`;

    console.log("Enviando petición a GitHub API...");

    try {
        const response = await fetch(url, {
            method: "POST",
            headers: {
                Authorization: `Bearer ${GITHUB_TOKEN}`,
                Accept: "application/vnd.github+json",
                "Content-Type": "application/json",
                "X-GitHub-Api-Version": "2022-11-28"
            },
            body: JSON.stringify({
                ref: "main"
            })
        });

        if (response.ok) {
            console.log("✅ Workflow disparado con éxito.");
        } else {
            const errorData = await response.json();
            console.error(`❌ Error al disparar el workflow: ${response.status} - ${errorData.message}`);
        }
    } catch (error) {
        console.error(`❌ Error de red o ejecución: ${error.message}`);
    }
}

dispararWorkflow();
