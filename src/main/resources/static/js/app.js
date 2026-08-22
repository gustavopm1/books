import { createBook, deleteBook, getBooks } from "./api.js";

const state = {
    books: [],
    search: "",
    genre: "",
    page: 0,
    size: 20,
    totalItems: 0,
    totalPages: 0,
    loading: false
};

const elements = {
    searchInput: document.querySelector("#search-input"),
    genreSelect: document.querySelector("#genre-select"),
    clearFiltersButton: document.querySelector("#clear-filters-button"),
    resultsSummary: document.querySelector("#results-summary"),
    loadingStatus: document.querySelector("#loading-status"),
    errorBanner: document.querySelector("#error-banner"),
    errorMessage: document.querySelector("#error-message"),
    retryButton: document.querySelector("#retry-button"),
    booksTable: document.querySelector("#books-table"),
    booksBody: document.querySelector("#books-body"),
    emptyState: document.querySelector("#empty-state"),
    previousPageButton: document.querySelector("#previous-page-button"),
    nextPageButton: document.querySelector("#next-page-button"),
    pageSummary: document.querySelector("#page-summary"),
    dialog: document.querySelector("#book-dialog"),
    openFormButton: document.querySelector("#open-form-button"),
    closeFormButton: document.querySelector("#close-form-button"),
    cancelFormButton: document.querySelector("#cancel-form-button"),
    form: document.querySelector("#book-form"),
    formError: document.querySelector("#form-error"),
    submitFormButton: document.querySelector("#submit-form-button"),
    deleteDialog: document.querySelector("#delete-dialog"),
    deleteDialogMessage: document.querySelector("#delete-dialog-message"),
    deleteError: document.querySelector("#delete-error"),
    cancelDeleteButton: document.querySelector("#cancel-delete-button"),
    confirmDeleteButton: document.querySelector("#confirm-delete-button"),
    toast: document.querySelector("#toast")
};

let activeRequest;
let searchTimer;
let toastTimer;
let pendingDelete;
let deleteInProgress = false;

async function loadBooks() {
    activeRequest?.abort();
    const requestController = new AbortController();
    activeRequest = requestController;
    setLoading(true);
    hideError();

    try {
        const response = await getBooks({
            search: state.search,
            genre: state.genre,
            page: state.page,
            size: state.size,
            signal: requestController.signal
        });

        state.books = response.items;
        state.page = response.page;
        state.size = response.size;
        state.totalItems = response.totalItems;
        state.totalPages = response.totalPages;
        render();
    } catch (error) {
        if (error.name !== "AbortError") {
            showError(error.message);
        }
    } finally {
        if (activeRequest === requestController) {
            setLoading(false);
        }
    }
}

function render() {
    elements.booksBody.replaceChildren(...state.books.map(createBookRow));

    const hasBooks = state.books.length > 0;
    elements.booksTable.hidden = !hasBooks;
    elements.emptyState.hidden = hasBooks;

    if (hasBooks) {
        const firstItem = state.page * state.size + 1;
        const lastItem = firstItem + state.books.length - 1;
        elements.resultsSummary.textContent = `Showing ${firstItem}-${lastItem} of ${state.totalItems} books`;
    } else {
        elements.resultsSummary.textContent = "No matching books";
    }

    const displayedPage = state.totalPages === 0 ? 0 : state.page + 1;
    elements.pageSummary.textContent = `Page ${displayedPage} of ${state.totalPages}`;
    elements.previousPageButton.disabled = state.loading || state.page === 0;
    elements.nextPageButton.disabled = state.loading || state.page + 1 >= state.totalPages;
    elements.clearFiltersButton.hidden = !state.search && !state.genre;
}

function createBookRow(book) {
    const row = document.createElement("tr");
    row.append(
        createCell("Title", book.title),
        createCell("Author", book.author),
        createGenreCell(book.genre),
        createCell("Year", book.publicationYear ?? "-"),
        createActionCell(book)
    );
    return row;
}

function createCell(label, value) {
    const cell = document.createElement("td");
    cell.dataset.label = label;
    cell.textContent = value;
    return cell;
}

function createGenreCell(genre) {
    const cell = document.createElement("td");
    const pill = document.createElement("span");
    cell.dataset.label = "Genre";
    pill.className = "genre-pill";
    pill.textContent = genre;
    cell.append(pill);
    return cell;
}

function createActionCell(book) {
    const cell = document.createElement("td");
    const button = document.createElement("button");
    button.type = "button";
    button.className = "button delete-button";
    button.textContent = "Delete";
    button.setAttribute("aria-label", `Delete ${book.title}`);
    button.addEventListener("click", () => openDeleteDialog(book, button));
    cell.append(button);
    return cell;
}

function openDeleteDialog(book, button) {
    pendingDelete = {book, button};
    elements.deleteDialogMessage.textContent = `“${book.title}” by ${book.author} will be permanently removed from the catalogue.`;
    elements.deleteError.hidden = true;
    elements.deleteDialog.showModal();
}

async function confirmDelete() {
    if (!pendingDelete || deleteInProgress) {
        return;
    }

    const {book, button} = pendingDelete;
    deleteInProgress = true;
    button.disabled = true;
    button.textContent = "Deleting...";
    elements.cancelDeleteButton.disabled = true;
    elements.confirmDeleteButton.disabled = true;
    elements.confirmDeleteButton.textContent = "Deleting...";
    elements.deleteError.hidden = true;

    try {
        await deleteBook(book.id);
        if (state.books.length === 1 && state.page > 0) {
            state.page -= 1;
        }
        elements.deleteDialog.close();
        showToast(`Deleted "${book.title}".`);
        await loadBooks();
    } catch (error) {
        button.disabled = false;
        button.textContent = "Delete";
        elements.deleteError.textContent = error.message;
        elements.deleteError.hidden = false;
    } finally {
        deleteInProgress = false;
        elements.cancelDeleteButton.disabled = false;
        elements.confirmDeleteButton.disabled = false;
        elements.confirmDeleteButton.textContent = "Delete book";
    }
}

function scheduleSearchUpdate() {
    window.clearTimeout(searchTimer);
    searchTimer = window.setTimeout(() => {
        state.search = elements.searchInput.value.trim();
        state.page = 0;
        loadBooks();
    }, 300);
}

function updateGenreFilter() {
    state.genre = elements.genreSelect.value;
    state.page = 0;
    loadBooks();
}

async function submitBook(event) {
    event.preventDefault();
    elements.formError.hidden = true;

    if (!elements.form.reportValidity()) {
        return;
    }

    const formData = new FormData(elements.form);
    const year = formData.get("publicationYear").trim();
    const book = {
        title: formData.get("title").trim(),
        author: formData.get("author").trim(),
        genre: formData.get("genre").trim(),
        publicationYear: year ? Number(year) : null
    };

    elements.submitFormButton.disabled = true;
    elements.submitFormButton.textContent = "Adding...";

    try {
        await createBook(book);
        elements.form.reset();
        elements.dialog.close();
        state.page = 0;
        showToast(`Added "${book.title}".`);
        await loadBooks();
    } catch (error) {
        elements.formError.textContent = error.message;
        elements.formError.hidden = false;
    } finally {
        elements.submitFormButton.disabled = false;
        elements.submitFormButton.textContent = "Add book";
    }
}

function setLoading(loading) {
    state.loading = loading;
    elements.loadingStatus.textContent = loading ? "Loading..." : "";
    elements.booksTable.setAttribute("aria-busy", String(loading));
    elements.previousPageButton.disabled = loading || state.page === 0;
    elements.nextPageButton.disabled = loading || state.page + 1 >= state.totalPages;
}

function showError(message) {
    elements.errorMessage.textContent = message;
    elements.errorBanner.hidden = false;
}

function hideError() {
    elements.errorBanner.hidden = true;
}

function showToast(message) {
    window.clearTimeout(toastTimer);
    elements.toast.textContent = message;
    elements.toast.hidden = false;
    toastTimer = window.setTimeout(() => {
        elements.toast.hidden = true;
    }, 3500);
}

elements.searchInput.addEventListener("input", scheduleSearchUpdate);
elements.genreSelect.addEventListener("change", updateGenreFilter);
elements.clearFiltersButton.addEventListener("click", () => {
    elements.searchInput.value = "";
    elements.genreSelect.value = "";
    state.search = "";
    state.genre = "";
    state.page = 0;
    loadBooks();
});
elements.previousPageButton.addEventListener("click", () => {
    state.page -= 1;
    loadBooks();
});
elements.nextPageButton.addEventListener("click", () => {
    state.page += 1;
    loadBooks();
});
elements.retryButton.addEventListener("click", loadBooks);
elements.openFormButton.addEventListener("click", () => elements.dialog.showModal());
elements.closeFormButton.addEventListener("click", () => elements.dialog.close());
elements.cancelFormButton.addEventListener("click", () => elements.dialog.close());
elements.form.addEventListener("submit", submitBook);
elements.cancelDeleteButton.addEventListener("click", () => {
    if (!deleteInProgress) {
        elements.deleteDialog.close();
    }
});
elements.confirmDeleteButton.addEventListener("click", confirmDelete);
elements.deleteDialog.addEventListener("cancel", (event) => {
    if (deleteInProgress) {
        event.preventDefault();
    }
});
elements.deleteDialog.addEventListener("close", () => {
    pendingDelete = undefined;
    elements.deleteError.hidden = true;
});

loadBooks();
