# Smiles2Monomers
A tool to link chemical structures to biological structures

Resear article :  
"Smiles2Monomers : a link between chemical and biological structures for polymers"  
Yoann Dufresne, Laurent Noé, Valérie Leclère and Maude Pupin
http://www.ncbi.nlm.nih.gov/pubmed/26715946
Journal of Cheminformatics, January 2016

## Overview

Smiles2Monomers is a software to infer monomeric structures from atomic structures of polymers. The structures are predicted by a two-step algorithm. First, all the candidates monomers are independantly searched and second, all the founded monomers are putted together in a non-overlapping tiling. For more details, please read the article.  
For speed performances during the search, we decided to pre-compute an index of the best way to search each monomer in polymers. So, each monomer have to be carve by boundary rules and ordered using frequences of atoms.

To perform an analysis of polymers a few files are required:
* A monomers file: The candidates monomers in the polymers
* A polymers file: The polymers to analyse
* A rules file: The rules to link monomers together
* A learning file: This file has to contain "classical" polymers for your domain. The composition will impact the speed of the seach but not the quality of the results.

For details of the files compositions please read the [wiki page](https://github.com/yoann-dufresne/Smiles2Monomers/wiki/Json-formats).

## How to install (Command line procedure)

### Download
`git clone https://github.com/yoann-dufresne/Smiles2Monomers`  
`cd Smiles2Monomers`

### Compile
`ant compile`  
`ant create_jar`

Now you should find a binary file named s2m.jar in the build directory. The program is ready.

## How to use s2m

### Using the ant file

#### Pre-computation

Files requirments:
* data/monomers.json: The monomers to search for
* data/rules.json: The binding rules
* data/learning.json: Learning base of polymers

When these files are ready, you are ready to pre-compute the monomer index:  
`ant preCompute`

To verify if nothing gone wrong, go into the data directory and look for residues.json, chains.json and serials/ directory. If they all exists, you can now run the main program on many polymer files.

#### Running s2m

Files requirments:
* data/monomers.json: The monomers to search for
* data/polymers.json: The polymers of interest
* data/residues.json: Generated by the pre-computation
* data/chains.json: Generated by the pre-computation
* data/serials/: Generated by the pre-computation

When all the files are ready you can tip  
`ant s2m`

### Java command line

Work in progress
