# Quick start

GreyCat is distributed over maven repository.
Therefore it can be used with maven, ivy, sbt...
Simply create a new project and add the api and core module dependency to your project and start using GreyCat:

``` xml
<dependency>
	<groupId>com.datathings</groupId>
	<artifactId>greycat</artifactId>
	<version>${greycat.version}</version>
</dependency>
```

If you prefer to code in JavaScript or TypeScript we also distribute GreyCat over NPM central repository.
Into your NPM project just use the following:

``` sh
npm -i greycat --save
```

All examples and demos found in this tutorial can be downloaded from this [public github repository](https://github.com/datathings/greycat-samples)

Next let's start by showing a minimal example of a static [graph usage](minimal.md).
