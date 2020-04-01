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
        "navbar": "var(--color-navbar)",
        "primary-button": "var(--color-primary-button)",
        "secondary-button": "var(--color-secondary-button)",
        "text-color": "var(--color-text)",
        "transparent-black": "rgba(0,0,0,.6)",
        "danger-button": "var(--color-red-button)",
      },
      minWidth: {
        '1/4': '24%',
        '1/3': '40%',
        'unset': 'unset',
        '200': '400px',
        'psm': '350px',
        'pmd': '350px',
        'plg': '350px'
      },
      height: {
        '91%': '91%',
        '82%': '82%',
      },
      width: {
        '10': '10%',
        '1/11': '13%',
        '12': '12%',
        '15': '15%',
        '22': '22%',
        '1/4': '22%',
        '1/3': '31%',
        '1/2': '47%'
      },
      minHeight: {
        '10': '130px',
        '15': '200px',
        '100': '100%',
        'psm': '200px',
        'pmd': '200px',
        'plg': '200px'
      },
      maxHeight: {
        '50': '50%',
        '200': '200px',
        '250px': '250px',
        'psm': '200px',
        'pmd': '220px',
        'plg': '250px'
      },
      maxWidth: {
        '200px': '200px',
        '20': '400px',
        '250px': '250px',
        'psm': '350px',
        'pmd': '400px',
        'plg': '400px'
      },
      margin:{
        '-200px': '200px',
      }
    }
  },
  variants: {},
  plugins: []
}
