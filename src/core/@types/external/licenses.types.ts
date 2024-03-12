export interface JavaLicenses {
  dependencies: JavaDependency[]
}

export interface JavaDependency {
  name: string
  file: string
  licenses: JavaLicense[]
}

export interface JavaLicense {
  name: string
  url?: string
}

export interface JavascriptLicenses {
  [key: string]: JavascriptDependency
}

export interface JavascriptDependency {
  licenses: string
  repository: string
  path: string
  licenseFile: string
}