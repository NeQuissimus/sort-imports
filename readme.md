# SortImports

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

`scalafixDependencies += "com.nequissimus" %% "sort-imports" % "0.2.1"`

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
