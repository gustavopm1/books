const API_URL = "/api/books";

async function request(url, options = {}) {
    const response = await fetch(url, {
        ...options,
        headers: {
            Accept: "application/json",
            ...options.headers
        }
    });

    if (!response.ok) {
        const body = await response.json().catch(() => ({}));
        throw new Error(body.error ?? body.detail ?? "The request could not be completed.");
    }

    return response.status === 204 ? null : response.json();
}

export function getBooks({ search, genre, page, size, signal }) {
    const parameters = new URLSearchParams({
        page: String(page),
        size: String(size)
    });

    if (search) {
        parameters.set("search", search);
    }

    if (genre) {
        parameters.set("genre", genre);
    }

    return request(`${API_URL}?${parameters}`, { signal });
}

export function createBook(book) {
    return request(API_URL, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(book)
    });
}

export function deleteBook(id) {
    return request(`${API_URL}/${id}`, { method: "DELETE" });
}
