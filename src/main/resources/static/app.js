// URL del backend: la define config.js (en Vercel se genera con la variable API_URL).
// Si está vacía, se usa el mismo origen (frontend servido por Spring Boot en /).
const BACKEND_URL = window.API_URL || window.location.origin;
const API_BASE = new URL("/api/reservas", BACKEND_URL);
const API_VERSION = "1.0";


class ApiError extends Error {
    constructor(status, title, detail) {
        super(detail || title || `Error HTTP ${status}`);
        this.name = "ApiError";
        this.status = status;
        this.title = title || `Error ${status}`;
        this.detail = detail || "No fue posible completar la solicitud.";
    }
}

async function apiFetch(url, options = {}) {
    let response;
    try {
        response = await fetch(url, {
            ...options,
            headers: {
                "X-API-Version": API_VERSION,
                ...(options.body ? { "Content-Type": "application/json" } : {}),
                ...options.headers,
            },
        });
    } catch {
        throw new ApiError(0, "Sin conexión", "No se pudo conectar con el servidor. Inténtalo de nuevo.");
    }

    if (!response.ok) {
        let problem = null;
        const contentType = response.headers.get("content-type") || "";
        if (contentType.includes("json")) {
            try {
                problem = await response.json();
            } catch {
                problem = null;
            }
        }
        throw new ApiError(
            response.status,
            problem?.title || `Error ${response.status}`,
            problem?.detail || `La solicitud falló (HTTP ${response.status}).`,
        );
    }

    if (response.status === 204) {
        return { data: null, response };
    }
    return { data: await response.json(), response };
}

function reservaUrl(codigo) {
    const url = new URL(API_BASE);
    url.searchParams.set("codigo", codigo);
    return url;
}

async function consultarReserva(codigo) {
    const { data } = await apiFetch(reservaUrl(codigo));
    return data;
}

async function crearReserva(datos) {
    return apiFetch(API_BASE, {
        method: "POST",
        body: JSON.stringify(datos),
    });
}

function selfHref(reserva) {
    const links = reserva?._links;
    if (Array.isArray(links)) {
        return links.find((link) => link?.rel === "self")?.href;
    }
    return links?.self?.href;
}

function reservaConsultaHref(href, codigo) {
    if (href) {
        return new URL(href, window.location.origin);
    }
    return reservaUrl(codigo);
}

function mostrarMensaje(element, { title, detail, status, success = false }) {
    element.replaceChildren();
    element.className = "message";
    if (success) {
        element.classList.add("success");
    } else if (status === 409) {
        element.classList.add("conflict");
    } else {
        element.classList.add("error");
    }

    const heading = document.createElement("strong");
    heading.textContent = status ? `${title} · ${status}` : title;
    const body = document.createElement("span");
    body.textContent = detail;
    element.append(heading, body);
    element.hidden = false;
}

function limpiarEstado(message, result) {
    message.hidden = true;
    message.replaceChildren();
    result.hidden = true;
    result.replaceChildren();
}

function crearDetalle(label, value) {
    const wrapper = document.createElement("div");
    const term = document.createElement("dt");
    const description = document.createElement("dd");
    term.textContent = label;
    description.textContent = value ?? "—";
    wrapper.append(term, description);
    return wrapper;
}

function mostrarReserva(container, reserva, { location, allowLookup = false } = {}) {
    container.replaceChildren();
    const heading = document.createElement("h3");
    heading.textContent = `Reserva ${reserva.codigoReserva}`;

    const details = document.createElement("dl");
    details.append(
        crearDetalle("Cliente", reserva.cliente?.nombre),
        crearDetalle("Documento", reserva.cliente?.documento),
        crearDetalle("Espectáculo", reserva.espectaculo?.nombre),
        crearDetalle("Fecha", reserva.espectaculo?.fecha),
        crearDetalle("Sillas", reserva.cantidadSillas),
    );
    container.append(heading, details);

    if (allowLookup) {
        const button = document.createElement("button");
        button.className = "button button-dark";
        button.type = "button";
        button.textContent = "Ver mi reserva";
        button.addEventListener("click", async () => {
            const lookupMessage = document.querySelector("#lookup-message");
            const lookupResult = document.querySelector("#lookup-result");
            const href = location || selfHref(reserva);
            await ejecutarConsulta(reserva.codigoReserva, {
                url: reservaConsultaHref(href, reserva.codigoReserva),
                message: lookupMessage,
                result: lookupResult,
            });
        });
        container.append(button);
    }
    container.hidden = false;
}

async function ejecutarConsulta(codigo, { url, message, result }) {
    limpiarEstado(message, result);
    try {
        const reserva = url
            ? (await apiFetch(url)).data
            : await consultarReserva(codigo);
        mostrarReserva(result, reserva);
    } catch (error) {
        const apiError = error instanceof ApiError
            ? error
            : new ApiError(0, "Error inesperado", "No fue posible consultar la reserva.");
        mostrarMensaje(message, {
            title: apiError.title,
            detail: apiError.detail,
            status: apiError.status,
        });
    }
}

const lookupForm = document.querySelector("#lookup-form");
const lookupMessage = document.querySelector("#lookup-message");
const lookupResult = document.querySelector("#lookup-result");
const lookupButton = lookupForm.querySelector("button");

lookupForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    const codigo = new FormData(lookupForm).get("codigo").trim();
    if (!codigo) {
        mostrarMensaje(lookupMessage, {
            title: "Código requerido",
            detail: "Ingresa el código de tu reserva.",
            status: 400,
        });
        return;
    }

    lookupButton.disabled = true;
    lookupButton.textContent = "Consultando…";
    await ejecutarConsulta(codigo, {
        message: lookupMessage,
        result: lookupResult,
    });
    lookupButton.disabled = false;
    lookupButton.innerHTML = 'Consultar reserva <span aria-hidden="true">→</span>';
});

const createForm = document.querySelector("#create-form");
const createMessage = document.querySelector("#create-message");
const createResult = document.querySelector("#create-result");
const createButton = createForm.querySelector("button");

createForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    limpiarEstado(createMessage, createResult);

    const fields = new FormData(createForm);
    const datos = {
        clienteId: Number(fields.get("clienteId")),
        espectaculoId: Number(fields.get("espectaculoId")),
        cantidadSillas: Number(fields.get("cantidadSillas")),
    };

    if (!Number.isInteger(datos.clienteId) || datos.clienteId < 1
        || !Number.isInteger(datos.espectaculoId) || datos.espectaculoId < 1
        || !Number.isInteger(datos.cantidadSillas) || datos.cantidadSillas < 1 || datos.cantidadSillas > 10) {
        mostrarMensaje(createMessage, {
            title: "Revisa los datos",
            detail: "Los IDs deben ser enteros positivos y la cantidad debe estar entre 1 y 10.",
            status: 400,
        });
        return;
    }

    createButton.disabled = true;
    createButton.textContent = "Creando reserva…";
    try {
        const { data: reserva, response } = await crearReserva(datos);
        const href = response.headers.get("Location") || selfHref(reserva);
        mostrarMensaje(createMessage, {
            title: "¡Reserva creada!",
            detail: `Tu código de reserva es ${reserva.codigoReserva}.`,
            success: true,
        });
        mostrarReserva(createResult, reserva, { location: href, allowLookup: true });
    } catch (error) {
        const apiError = error instanceof ApiError
            ? error
            : new ApiError(0, "Error inesperado", "No fue posible crear la reserva.");
        mostrarMensaje(createMessage, {
            title: apiError.title,
            detail: apiError.detail,
            status: apiError.status,
        });
    } finally {
        createButton.disabled = false;
        createButton.innerHTML = 'Crear mi reserva <span aria-hidden="true">→</span>';
    }
});
