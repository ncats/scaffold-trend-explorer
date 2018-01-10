# Scaffold Trend Explorer

This application allows a user to visualize trends in scaffold-based properties, in a manner analogous to
[Google Trends](https://trends.google.com/trends/). A more in depth discussion of scaffold trends can be found in [Zdrazil and Guha (2017)](http://pubs.acs.org/doi/abs/10.1021/acs.jmedchem.7b00954).

The application is built using the [Play](https://www.playframework.com/) framework and requires a [ChEMBL](https://www.ebi.ac.uk/chembl/) instance hosted in a PostgreSQL database that has the [RDKit](http://www.rdkit.org/) cartridge installed.

## Building

The code can be built and run locally by doing
```
sbt run
```
The application should be available at `http://127.0.0.1:9000`

## Deploying

To generate a distribution, run
```
sbt dist
```
which will generate a ZIP file under `target/universal`, which can then be deployed
