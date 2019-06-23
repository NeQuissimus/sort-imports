# SortImports

## Usage

`scalafixDependencies += "com.nequissimus" %% "sort-imports" % "0.1.0"`

## Configuration

```
rule = SortImports
SortImports.blocks = [
  "java",
  "scala",
  "*",
  "com.sun"
]
```
