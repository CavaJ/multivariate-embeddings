## First specify the packages of interest
packages <- c("shiny", "shinythemes", "shinyFiles",
              "dplyr", "readr", "ggplot2", "stringr", "rJava")

## Now load or install&load all
package.check <- lapply(
  packages,
  FUN = function(x) {
    if (!require(x, character.only = TRUE)) {
      install.packages(x, dependencies = TRUE)
      library(x, character.only = TRUE)
    }
  }
)


# Load packages
#library(shiny)
#library(shinythemes)
#library(dplyr)
#library(readr)
##library(ggplot2)

#The default heap size for libraries that rely on rJava is 512MB. It is relatively easy to exceed this maximum size.
#library(rJava)
#options(java.parameters = "-Xmx1024m")
.jinit(parameters="-Xmx2048m")


start_time <- proc.time()


#javaImport(packages = "com.rb.me.*")

#print(.jclassPath())

#s <- .jnew("java/lang/String", "Hello World!")

#print(s)

#.jcall(s,"I","length")
#s$length()

#getwd()

#.jclassPath() # "C:\\Users\\rbabayev\\Documents\\R\\R-3.6.3\\library\\rJava\\java"

#.jaddClassPath("weka_stable_3.8.4")
#.jaddClassPath('out/production/Multivariate Embeddings')
#.jaddClassPath("out/production/multiInstanceFilters")
#.jaddClassPath("out/production/multiInstanceLearning")
.jaddClassPath("artifacts/Multivariate Embeddings.jar")

#obj <- .jnew("com.rb.me.Launcher")

#J("com.rb.me.Launcher")$main()

#.jcall(obj, "Ljava/lang/String;", "SayMyName")

#obj$main(c("Hello", "Hello"))

#obj$SayMyName()
#.jcall(obj, returnSig="Ljava/lang/String;", method="SayMyName") #, 'resources/new.txt')


obj <- .jnew("com.rb.me.Utils")


mtses <- .jcall(obj, returnSig = "Ljava/lang/Object;", method="mtsesFromSetA", "data/set-a", "resources/variable_ranges_physionet.csv")

#x <- .jcall(obj, returnSig="Ljava/lang/String;", method="longestMTSEInSetA", "mean", mtses)

#print(x)

#quit(status = 1)



x_mean <- .jcall(obj, returnSig="Ljava/lang/String;", method="longestMTSEInSetA", "mean", mtses, "resources/variable_ranges_physionet.csv")
df_mean <- read.table(text = x_mean, sep =",", header = TRUE, stringsAsFactors = FALSE)


x_forward <- .jcall(obj, returnSig="Ljava/lang/String;", method="longestMTSEInSetA", "forward", mtses, "resources/variable_ranges_physionet.csv")
df_forward <- read.table(text = x_forward, sep =",", header = TRUE, stringsAsFactors = FALSE)


x_zero <- .jcall(obj, returnSig="Ljava/lang/String;", method="longestMTSEInSetA", "zero", mtses, "resources/variable_ranges_physionet.csv")
df_zero <- read.table(text = x_zero, sep =",", header = TRUE, stringsAsFactors = FALSE)


x_normal <- .jcall(obj, returnSig="Ljava/lang/String;", method="longestMTSEInSetA", "normal_value", mtses, "resources/variable_ranges_physionet.csv")
df_normal <- read.table(text = x_normal, sep =",", header = TRUE, stringsAsFactors = FALSE)




vars <- setdiff(names(df_mean), "tsMinutes")
#print(vars)
#length <- nrow(df_mean)

#print(df[, c("tsMinutes", "ALP")])
#print(df[, "ALP"])


#quit(status=1)

ui <- fluidPage(
  titlePanel(h2("Health Monitoring Data (PhysioNet Dataset)",align = "center")),
  sidebarLayout(
    sidebarPanel(

      # shinyDirButton("dir", label = "Input directory", title="Upload"),
      # #verbatimTextOutput("dir", placeholder = TRUE)

      selectInput(inputId = "imputation_method",
                  label = "Choose an imputation method:",
                  choices = c("Mean", "Forward", "Zero", "Normal_Value"),
                  selected = "Mean"),

      selectInput(inputId = "dataset",
                  label = "Choose a variable:",
                  choices = vars,
                  selected = vars[0])),
    mainPanel(
      plotOutput("ts_plot"),
      verbatimTextOutput("summary"))))


server <- shinyServer(

  function(input,output)
    {

    # shinyDirChoose(
    #   input,
    #   'dir',
    #   roots = c(home = '~'),
    #   filetypes = c('', 'txt', 'bigWig', "tsv", "csv", "bw")
    # )
    #
    # global <- reactiveValues(datapath = getwd())
    #
    # dir <- reactive(input$dir)


    datasetInput <- reactive({
                              if (input$imputation_method == "Mean")
                               df_mean[, c("tsMinutes", input$dataset)] #df %>% filter(value_name == input$dataset)
                              else if(input$imputation_method == "Forward")
                                df_forward[, c("tsMinutes", input$dataset)]
                              else if(input$imputation_method == "Zero")
                                df_zero[, c("tsMinutes", input$dataset)]
                              else
                                df_normal[, c("tsMinutes", input$dataset)]
                             })


    # Generate a summary of the dataset ----
    #output$summary <- renderPrint({
    #                                dataset <- datasetInput()
    #                                summary(dataset$result)
    #                              })

    # plot time series
    output$ts_plot <- renderPlot({

                                   dataset <- datasetInput()
                                   #ggplot(dataset, aes(x = "tsMinutes", y=input$dataset)) + geom_line()
                                   #ggplot(dataset, aes(x = dataset[, "tsMinutes"], y=dataset[, input$dataset]))

                                   plot(dataset, type="l", col="green", lwd=3, pch=15,
                                        )
                                 })
  })



end_time <- proc.time()

print((end_time - start_time))


shinyApp(ui = ui, server = server)
