#FROM rocker/shiny:latest

#FROM rocker/shiny-verse:latest

FROM r-base:latest


# install wget and gnupg
#RUN apt-get update && apt-get install -my wget gnupg


# system libraries of general use
RUN apt-get update && apt-get install -y \
    sudo \
    gdebi-core \
    pandoc \
    pandoc-citeproc \
    libcurl4-gnutls-dev \
    libcairo2-dev \
    libxt-dev \
    libssl-dev \
    libssh2-1-dev \
    libicu-dev \
    libbz2-dev \
    liblzma-dev \
    xtail



# Add shiny user (required in windows 7)
#RUN groupadd  shiny \
#&& useradd --gid shiny --shell /bin/bash --create-home shiny



# clean local repository
RUN apt-get clean


# install java 8 and set java configuration for R (rJava)
RUN apt-get update \
    && apt-get install -y openjdk-8-jdk openjdk-8-jre \
    && R CMD javareconf #JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/

# clean local repository
RUN apt-get clean

# set up JAVA_HOME
#ENV JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/



# Download and install ShinyServer
RUN apt-get update \
    && wget --no-verbose https://download3.rstudio.org/ubuntu-14.04/x86_64/shiny-server-1.5.14.948-amd64.deb \
    && dpkg -i shiny-server-1.5.14.948-amd64.deb


# clean local repository
RUN apt-get clean


# install R packages required 
# (change it dependeing on the packages you need)
RUN R -e "install.packages('rJava', repos='http://cran.rstudio.com/')"
RUN R -e "install.packages('shiny', repos='http://cran.rstudio.com/')"
RUN R -e "install.packages('shinydashboard', repos='http://cran.rstudio.com/')"
#RUN R -e "devtools::install_github('andrewsali/shinycssloaders')"
RUN R -e "install.packages('shinythemes', repos='http://cran.rstudio.com/')"
RUN R -e "install.packages('dplyr', repos='http://cran.rstudio.com/')"
RUN R -e "install.packages('readr', repos='http://cran.rstudio.com/')"
RUN R -e "install.packages('ggplot2', repos='http://cran.rstudio.com/')"
RUN R -e "install.packages('stringr', repos='http://cran.rstudio.com/')"


# Copy configuration files into the Docker image
COPY shiny-server.conf  /etc/shiny-server/shiny-server.conf 

# copy the app to the image
COPY app1/app.R /srv/shiny-server/app1/
COPY app2/app.R /srv/shiny-server/app2/
COPY data /srv/shiny-server/data
COPY resources /srv/shiny-server/resources
COPY artifacts /srv/shiny-server/artifacts


# select port
#EXPOSE 3839


# Copy further configuration files into the Docker image
COPY shiny-server.sh /usr/bin/shiny-server.sh


# allow permission
#RUN chown -R shiny:shiny /srv/shiny-server


# make shiny-server.sh executable
RUN ["chmod", "+x", "/usr/bin/shiny-server.sh"]


# run app
CMD ["/usr/bin/shiny-server.sh"]



#chmod our folder so that the files can be run by the container
#RUN chmod -R +rx /srv/shiny-server/

#USER shiny EXPOSE 80 CMD ["/usr/bin/shiny-server.sh"]






