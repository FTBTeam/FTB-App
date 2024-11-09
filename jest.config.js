module.exports = {
  moduleFileExtensions: ['js', 'json', 'vue', 'ts'],
  transform: {
    '^.+\\.vue$': 'vue-jest',
    '.+\\.(css|styl|less|sass|scss|svg|png|jpg|ttf|woff|woff2)$': 'jest-transform-stub',
    '^.+\\.tsx?$': 'ts-jest',
  },
  transformIgnorePatterns: ['/node_modules/', '/dist_electron/'],
  testMatch: ['**/*.test.(js|jsx|ts|tsx)'],
  testEnvironmentOptions: {
    url: 'http://localhost/',
  },
  moduleNameMapper: {
    "^@\/(.*)$": "<rootDir>/src/$1"
  }
};
