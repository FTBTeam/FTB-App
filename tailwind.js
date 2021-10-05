module.exports = {
    theme: {
        screens: {
            'sm': '1000px',
            'md': '1440px',
            'lg': '1920px'
        },
        extend: {
            colors: {
                'sidebar-item': "var(--color-sidebar-item)",
                'sidebar-item-active': "var(--color-sidebar-item-active)",
                "background": "var(--color-background)",
                "background-lighten": "var(--color-background-lighten)",
                "background-darken": "var(--color-background-darken)",
                "navbar": "var(--color-navbar)",
                "primary": "var(--color-primary-button)",
                "light-primary": "var(--color-light-primary-button)",
                "secondary": "var(--color-secondary-button)",
                "light-secondary": "var(--color-light-secondary-button)",
                "warning": "var(--color-warning-button)",
                "light-warning": "var(--color-light-warning-button)",
                "info": "var(--color-info-button)",
                "light-info": "var(--color-light-info-button)",
                "text-color": "var(--color-text)",
                "transparent-black": "rgba(0,0,0,.6)",
                "danger": "var(--color-danger-button)",
                "light-danger": "var(--color-light-danger-button)",
            },
            minWidth: {
                '1/4': '25%',
                '1/2': '50%',
                '1/3': '40%',
                'unset': 'unset',
                '200': '400px',
                'psm': '350px',
                'pmd': '350px',
                'plg': '350px'
            },
            height: {
                '1/2': '50%',
                '91%': '91%',
                '82%': '82%',
                'size-1': '125px',
                'size-2': '200px',
                'size-3': '275px',
                'size-4': '350px',
                'size-5': '425px',
            },
            width: {
                '10': '10%',
                '1/11': '13%',
                '12': '12%',
                '15': '15%',
                '22': '22%',
                '1/4': '22%',
                '1/3': '31%',
                '1/2': '47%',
                'size-1': '125px',
                'size-2': '200px',
                'size-3': '275px',
                'size-4': '350px',
                'size-5': '425px',
            },
            minHeight: {
                '10': '130px',
                '15': '200px',
                '100': '100%',
                'psm': '200px',
                'pmd': '200px',
                'plg': '200px',
                'size-1': '48px',
                'size-2': '72px',
                'size-3': '90px',
                'size-4': '108px',
                'size-5': '144px',
            },
            maxHeight: {
                '50': '50%',
                '200': '200px',
                '250px': '250px',
                'psm': '200px',
                'pmd': '220px',
                'plg': '250px',
            },
            maxWidth: {
                '200px': '200px',
                '20': '400px',
                '250px': '250px',
                'psm': '350px',
                'pmd': '400px',
                'plg': '400px'
            },
            margin: {
                '-200px': '200px',
            },
            padding: {
                '0-5': '0.15rem',
            }
        }
    },
    // purge: [
    //   './src/**/*.html',
    //   './src/**/*.vue',
    // ],
    variants: {},
    plugins: []
}