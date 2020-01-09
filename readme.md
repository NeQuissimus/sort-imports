# SortImports

![GitHub Workflow Status](https://img.shields.io/github/workflow/status/NeQuissimus/sort-imports/Build)
![GitHub commits since latest release](https://img.shields.io/github/commits-since/NeQuissimus/sort-imports/latest/master)
![GitHub last commit](https://img.shields.io/github/last-commit/NeQuissimus/sort-imports)
![GitHub contributors](https://img.shields.io/github/contributors/NeQuissimus/sort-imports)

![GitHub tag (latest SemVer)](https://img.shields.io/github/v/tag/NeQuissimus/sort-imports?sort=semver)
![GitHub Release Date](https://img.shields.io/github/release-date/NeQuissimus/sort-imports)

[![Scala Steward badge](https://img.shields.io/badge/Scala_Steward-helping-blue.svg?style=flat&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAQCAMAAAARSr4IAAAAVFBMVEUAAACHjojlOy5NWlrKzcYRKjGFjIbp293YycuLa3pYY2LSqql4f3pCUFTgSjNodYRmcXUsPD/NTTbjRS+2jomhgnzNc223cGvZS0HaSD0XLjbaSjElhIr+AAAAAXRSTlMAQObYZgAAAHlJREFUCNdNyosOwyAIhWHAQS1Vt7a77/3fcxxdmv0xwmckutAR1nkm4ggbyEcg/wWmlGLDAA3oL50xi6fk5ffZ3E2E3QfZDCcCN2YtbEWZt+Drc6u6rlqv7Uk0LdKqqr5rk2UCRXOk0vmQKGfc94nOJyQjouF9H/wCc9gECEYfONoAAAAASUVORK5CYII=)](https://scala-steward.org)
![License](https://img.shields.io/github/license/NeQuissimus/sort-imports)

## Description

SortImports is a simplistic Scalafix rule.

It will organize imports into prefix-blocks and order imports inside those blocks alphabetically.

For example, these imports

```scala
import scala.util._
import scala.collection._
import java.util.Map
import com.oracle.net._
import com.sun._
```

will be organized as follows

```scala
import java.util.Map

import scala.collection._
import scala.util._

import com.oracle.net._

import com.sun._
```

if the blocks from the below _Configuration_ example are used.


**Important**
sort-imports does not (currently) take into account shadowing.
It is a faily dumb sorter of imports. If your code is using shadowing, it may end up no longer compiling!

## Usage

Latest version: ![GitHub tag (latest SemVer)](https://img.shields.io/github/v/tag/NeQuissimus/sort-imports?sort=semver)

`scalafixDependencies += "com.nequissimus" %% "sort-imports" % "<VERSION>"`

## Configuration

```
rule = SortImports
SortImports.blocks = [
  "java.",
  "scala.",
  "*",
  "com.sun."
]
```
