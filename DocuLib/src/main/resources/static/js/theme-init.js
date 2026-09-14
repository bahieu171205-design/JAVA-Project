(() => {
    let theme = 'light';
    try {
        if (window.localStorage.getItem('doculib-theme') === 'dark') theme = 'dark';
    } catch (error) {
        theme = 'light';
    }
    document.documentElement.dataset.theme = theme;
    document.documentElement.style.colorScheme = theme;
})();
