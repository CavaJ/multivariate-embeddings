# multivariate-embeddings


The main class is ```Launcher.java``` file. For reproducing the results, you should add this to your run configuration. 
The results are generated in [Physionet dataset](https://physionet.org/content/challenge-2012/1.0.0/). To run the experiments 
please update ```PHYSIONET_DATA_FOLDER``` constant in ```Constants.java``` file with a path to your own directory of Physionet dataset files.
It suffices to have the following structure in your own directory:

![directory structure](https://raw.githubusercontent.com/CavaJ/time-series-analysis/master/directory_structure.PNG)

The experiments will run in one-click execution style for train/test procedure on all learners. Note that, dependencies 
are based on Maven and available in ```Multivariate Embeddings.iml``` file.
