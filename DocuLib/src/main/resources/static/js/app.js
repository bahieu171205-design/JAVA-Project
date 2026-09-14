(() => {
    const body = document.body;
    const themeToggle = document.querySelector('[data-theme-toggle]');
    const themeColor = document.querySelector('[data-theme-color]');
    const applyTheme = (theme, persist = true) => {
        const dark = theme === 'dark';
        document.documentElement.dataset.theme = dark ? 'dark' : 'light';
        document.documentElement.style.colorScheme = dark ? 'dark' : 'light';
        if (themeColor) themeColor.content = dark ? '#111827' : '#ffffff';
        if (themeToggle) {
            themeToggle.setAttribute('aria-checked', String(dark));
            themeToggle.title = dark ? 'Chuyển sang giao diện sáng' : 'Chuyển sang giao diện tối';
            const label = themeToggle.querySelector('[data-theme-label]');
            if (label) label.textContent = dark ? 'Bật giao diện sáng' : 'Bật giao diện tối';
        }
        if (persist) {
            try {
                window.localStorage.setItem('doculib-theme', dark ? 'dark' : 'light');
            } catch (error) {
                // Giao diện vẫn đổi được nếu trình duyệt chặn localStorage.
            }
        }
    };
    applyTheme(document.documentElement.dataset.theme || 'light', false);
    themeToggle?.addEventListener('click', () => {
        applyTheme(document.documentElement.dataset.theme === 'dark' ? 'light' : 'dark');
    });

    const sidebar = document.querySelector('#sidebar');
    const sidebarOpeners = [...document.querySelectorAll('[data-sidebar-open]')];
    const mobileSidebar = window.matchMedia('(max-width: 1060px)');
    let sidebarTrigger = null;

    const sidebarFocusable = () => sidebar
        ? [...sidebar.querySelectorAll('a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])')]
        : [];

    const syncSidebarAccessibility = () => {
        if (!sidebar) return;
        const open = mobileSidebar.matches && body.classList.contains('sidebar-open');
        if (mobileSidebar.matches) {
            sidebar.toggleAttribute('inert', !open);
            sidebar.setAttribute('aria-hidden', String(!open));
        } else {
            body.classList.remove('sidebar-open');
            sidebar.removeAttribute('inert');
            sidebar.removeAttribute('aria-hidden');
        }
        sidebarOpeners.forEach((button) => button.setAttribute('aria-expanded', String(open)));
    };

    const openSidebar = (trigger) => {
        if (!sidebar || !mobileSidebar.matches) return;
        sidebarTrigger = trigger;
        body.classList.add('sidebar-open');
        syncSidebarAccessibility();
        window.requestAnimationFrame(() => {
            const closeButton = sidebar.querySelector('[data-sidebar-close]');
            (closeButton || sidebarFocusable()[0])?.focus();
        });
    };

    const closeSidebar = ({ restoreFocus = true } = {}) => {
        if (!sidebar || !mobileSidebar.matches) return;
        body.classList.remove('sidebar-open');
        syncSidebarAccessibility();
        if (restoreFocus) (sidebarTrigger || sidebarOpeners[0])?.focus();
        sidebarTrigger = null;
    };

    sidebarOpeners.forEach((button) => button.addEventListener('click', () => openSidebar(button)));
    document.querySelectorAll('[data-sidebar-close]').forEach((button) => {
        button.addEventListener('click', () => closeSidebar());
    });
    sidebar?.addEventListener('keydown', (event) => {
        if (event.key !== 'Tab' || !mobileSidebar.matches || !body.classList.contains('sidebar-open')) return;
        const focusable = sidebarFocusable();
        if (focusable.length === 0) return;
        const first = focusable[0];
        const last = focusable[focusable.length - 1];
        if (event.shiftKey && document.activeElement === first) {
            event.preventDefault();
            last.focus();
        } else if (!event.shiftKey && document.activeElement === last) {
            event.preventDefault();
            first.focus();
        }
    });
    mobileSidebar.addEventListener('change', syncSidebarAccessibility);
    syncSidebarAccessibility();
    document.addEventListener('keydown', (event) => {
        if (event.key === 'Escape') {
            if (body.classList.contains('sidebar-open')) closeSidebar();
            document.querySelectorAll('.action-menu[open]').forEach((menu) => menu.removeAttribute('open'));
            document.querySelectorAll('.user-menu[open]').forEach((menu) => menu.removeAttribute('open'));
        }
        if (event.key === '/' && !['INPUT', 'TEXTAREA', 'SELECT'].includes(document.activeElement.tagName)) {
            const search = document.querySelector('#global-search-input');
            if (search) {
                event.preventDefault();
                search.focus();
            }
        }
    });

    document.querySelectorAll('form[data-confirm]').forEach((form) => {
        form.addEventListener('submit', (event) => {
            if (!window.confirm(form.dataset.confirm)) event.preventDefault();
        });
    });
    document.querySelectorAll('[data-dismiss]').forEach((button) => {
        button.addEventListener('click', () => button.closest('.flash')?.remove());
    });

    document.querySelectorAll('[data-password-toggle]').forEach((button) => {
        button.addEventListener('click', () => {
            const input = button.parentElement?.querySelector('input');
            if (!input) return;
            const reveal = input.type === 'password';
            input.type = reveal ? 'text' : 'password';
            button.textContent = reveal ? 'Ẩn' : 'Hiện';
            button.setAttribute('aria-label', reveal ? 'Ẩn mật khẩu' : 'Hiện mật khẩu');
            button.setAttribute('aria-pressed', String(reveal));
        });
    });

    document.querySelectorAll('[data-switch-field]').forEach((field) => {
        const input = field.querySelector('input[type="checkbox"]');
        const status = field.querySelector('[data-switch-status]');
        if (!input || !status) return;
        const updateStatus = () => {
            status.textContent = input.checked
                ? 'Tài khoản đang được phép đăng nhập.'
                : 'Tài khoản đang bị khóa đăng nhập.';
        };
        input.addEventListener('change', updateStatus);
        updateStatus();
    });

    document.querySelectorAll('form').forEach((form) => {
        const status = form.querySelector('[data-workflow-status], select[name="status"]');
        const note = form.querySelector('[data-rejection-note], input.status-note, textarea[name="note"]');
        if (!status || !note || !form.action.includes('/acquisitions')) return;
        const syncRejectionReason = () => {
            const rejection = status.value === 'REJECTED';
            note.required = rejection;
            note.setAttribute('aria-required', String(rejection));
            if (rejection) {
                note.placeholder = 'Nhập lý do từ chối (bắt buộc)';
            }
        };
        status.addEventListener('change', syncRejectionReason);
        syncRejectionReason();
    });

    document.querySelectorAll('[data-filter-reset]').forEach((button) => {
        button.addEventListener('click', (event) => {
            event.preventDefault();
            const form = button.closest('form');
            if (form) window.location.assign(form.action);
        });
    });

    document.querySelectorAll('form[data-loading-form]').forEach((form) => {
        form.addEventListener('submit', (event) => {
            if (event.defaultPrevented) return;
            body.classList.add('is-loading');
            document.querySelector('[data-result-panel]')?.setAttribute('aria-busy', 'true');
            form.querySelectorAll('button[type="submit"]').forEach((button) => {
                if (!button.disabled) button.dataset.loadingDisabled = 'true';
                button.disabled = true;
            });
        });
    });

    const resetLoadingState = () => {
        body.classList.remove('is-loading');
        document.querySelectorAll('[data-result-panel][aria-busy="true"]').forEach((panel) => {
            panel.setAttribute('aria-busy', 'false');
        });
        document.querySelectorAll('[data-loading-disabled="true"]').forEach((button) => {
            button.disabled = false;
            delete button.dataset.loadingDisabled;
        });
    };

    document.addEventListener('click', (event) => {
        document.querySelectorAll('.action-menu[open]').forEach((menu) => {
            if (!menu.contains(event.target)) menu.removeAttribute('open');
        });
        document.querySelectorAll('.user-menu[open]').forEach((menu) => {
            if (!menu.contains(event.target)) menu.removeAttribute('open');
        });
    });

    window.addEventListener('pageshow', resetLoadingState);
})();
