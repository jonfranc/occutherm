# OccuTherm: Occupant Thermal Comfort Inference using Body Shape Information.

This is the official repository that implements the following paper:

> *Jonathan Francis&ast;, Matias Quintana&ast;, Nadine von Frankenberg, Sirajum Munir, and Mario Bergés. 2019. OccuTherm: Occupant Thermal Comfort Inference using Body Shape Information. In BuildSys '19: ACM International Conference on Systems for Energy-Efficient Buildings, Cities, and Transportation, November 13–14, 2019, New York, NY. ACM, New York, NY, USA, 10 pages. https://doi.org/10.1145/3360322.3360858*

# Getting started

## Obtain the OccuTherm dataset

Download the data here: [![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.3363986.svg)](https://doi.org/10.5281/zenodo.3363986)

```
>$ cd /path/to/dataset/files
>$ cat occutherm_dataset_v0-0-0.tar.gza* > archive.tar.gz
>$ tar -xvzf archive.tar.gz

>$ cd /path/to/repository
>$ ln -s /path/to/occutherm_dataset_v0-0-0/data data
```

## Feedback

Please send any feedback to jmf1@cs.cmu.edu and matias@u.nus.edu.

## Citation

If you use OccuTherm, please cite us as follows:

```
@inproceedings{francis_buildsys2019,
 author = {Francis, Jonathan and Quintana, Matias and von Frankenberg, Nadine and Munir, Sirajum and Berges, Mario},
 title = {OccuTherm: Occupant Thermal Comfort Inference using Body Shape Information},
 booktitle = {Proceedings of the 6th International Conference on Systems for Energy-Efficient Buildings, Cities, and Transportation},
 series = {BuildSys '19},
 year = {2019},
 isbn = {978-1-4503-7005-9/19/11},
 location = {New York, NY},
 numpages = {10},
 acmid = {3360858},
 publisher = {ACM},
 address = {New York, NY, USA},
 keywords = {Thermal Comfort, Human Studies, Machine Learning},
} 
```