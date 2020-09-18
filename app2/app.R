## First specify the packages of interest
packages <- c("shiny", "shinythemes",
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
#library(ggplot2)
#library(stringr)

#The default heap size for libraries that rely on rJava is 512MB. It is relatively easy to exceed this maximum size.
#library(rJava)
#options(java.parameters = "-Xmx1024m")
.jinit(parameters="-Xmx2048m")


start_time <- proc.time()


.jaddClassPath("../artifacts/Multivariate Embeddings.jar")

enum_values <- J("com.rb.me.TransformMethod")$values()

str_enum_values <- J("java.util.Arrays", method="toString", enum_values)

# remove TRANSFORM_
#str_enum_values <- str_replace_all(str_enum_values, "TRANSFORM_", "")

# remove brackets
str_enum_values <- str_replace_all(str_enum_values, "\\[|\\]", "")

# remove white space
str_enum_values <- str_replace_all(str_enum_values, " ", "")

# split string into single strings
enum_values <- str_split(str_enum_values, ",")[[1]]

# remove "FLAT" and "MIL"
enum_values <- setdiff(enum_values, c("FLAT", "MIL", "TRANSFORM_FLAT" , "TRANSFORM_MIL" ))

# obtain mtses
mtses <- J("com.rb.me.Utils", method="mtsesFromSetA", "../data/set-a", "../resources/variable_ranges_physionet.csv")


#start_time <- proc.time()

#x <- J("com.rb.me.Utils", method="transformMtsesToCSVString", "mean", "TRANSFORM_MEAN", mtses)

#print(x)

#end_time <- proc.time()

#print((end_time - start_time))


#df <- read.table(text = x, sep =",", header = TRUE, stringsAsFactors = FALSE)

# rename columns
#for (i in seq_along(names(df))) {
#  names(df)[i] <- strsplit(names(df)[i],"_")[[1]][1]
#}

#print(str_split("ALP_w_geo_mean", "_")[[1]][1])

#vars <- setdiff(names(df), "tsMinutes")

#print(vars)



#print(names(df))
#print(df_names[1])
#print(length(df_names))
#print(seq_along(df_names))

x_mean <- J("com.rb.me.Utils", method="transformMtsesToCSVString", "forward", "TRANSFORM_MEAN", mtses, "../resources/variable_ranges_physionet.csv")
df_mean <- read.table(text = x_mean, sep =",", header = TRUE, stringsAsFactors = FALSE)
# rename columns
for (i in seq_along(names(df_mean))) {
  names(df_mean)[i] <- strsplit(names(df_mean)[i],"_")[[1]][1]
}


x_w_mean <- J("com.rb.me.Utils", method="transformMtsesToCSVString", "forward", "TRANSFORM_WEIGHTED_MEAN", mtses, "../resources/variable_ranges_physionet.csv")
df_w_mean <- read.table(text = x_w_mean, sep =",", header = TRUE, stringsAsFactors = FALSE)
# rename columns
for (i in seq_along(names(df_w_mean))) {
  names(df_w_mean)[i] <- strsplit(names(df_w_mean)[i],"_")[[1]][1]
}


x_w_geo_mean <- J("com.rb.me.Utils", method="transformMtsesToCSVString", "forward", "TRANSFORM_WEIGHTED_GEOMETRIC_MEAN", mtses, "../resources/variable_ranges_physionet.csv")
df_w_geo_mean <- read.table(text = x_w_geo_mean, sep =",", header = TRUE, stringsAsFactors = FALSE)
# rename columns
for (i in seq_along(names(df_w_geo_mean))) {
  names(df_w_geo_mean)[i] <- strsplit(names(df_w_geo_mean)[i],"_")[[1]][1]
}


x_avg_power <- J("com.rb.me.Utils", method="transformMtsesToCSVString", "forward", "TRANSFORM_AVG_POWER", mtses, "../resources/variable_ranges_physionet.csv")
df_avg_power <- read.table(text = x_avg_power, sep =",", header = TRUE, stringsAsFactors = FALSE)
# rename columns
for (i in seq_along(names(df_avg_power))) {
  names(df_avg_power)[i] <- strsplit(names(df_avg_power)[i],"_")[[1]][1]
}


x_rms <- J("com.rb.me.Utils", method="transformMtsesToCSVString", "forward", "TRANSFORM_RMS", mtses, "../resources/variable_ranges_physionet.csv")
df_rms <- read.table(text = x_rms, sep =",", header = TRUE, stringsAsFactors = FALSE)
# rename columns
for (i in seq_along(names(df_rms))) {
  names(df_rms)[i] <- strsplit(names(df_rms)[i],"_")[[1]][1]
}



vars <- setdiff(names(df_mean), "tsMinutes")



ui <- pageWithSidebar(
  headerPanel('PhysioNet k-means clustering'),
  sidebarPanel(
    selectInput(inputId = "transform_method",
                label = "Choose a transformation method:",
                choices = c("TRANSFORM_MEAN", "TRANSFORM_WEIGHTED_MEAN", "TRANSFORM_WEIGHTED_GEOMETRIC_MEAN", "TRANSFORM_AVG_POWER", "TRANSFORM_RMS"),
                selected = "TRANSFORM_MEAN"),
    selectInput('xcol', 'X Variable', choices=vars, selected = vars[1]),
    selectInput('ycol', 'Y Variable', choices=vars, selected = vars[2]),
    numericInput('clusters', 'Cluster count', 2, min = 1, max = 9)
  ),
  mainPanel(
    plotOutput('plot1')
  )
)


server <- function(input, output, session) {

  # Combine the selected variables into a new data frame
  selectedData <- reactive({
                             if (input$transform_method == "TRANSFORM_MEAN")
                               df_mean[, c(input$xcol, input$ycol)]
                             else if(input$transform_method == "TRANSFORM_WEIGHTED_MEAN")
                               df_w_mean[, c(input$xcol, input$ycol)]
                             else if(input$transform_method == "TRANSFORM_WEIGHTED_GEOMETRIC_MEAN")
                               df_w_geo_mean[, c(input$xcol, input$ycol)]
                             else if(input$transform_method == "TRANSFORM_AVG_POWER")
                               df_avg_power[, c(input$xcol, input$ycol)]
                             else
                               df_rms[, c(input$xcol, input$ycol)]
                           })

  clusters <- reactive({
                         kmeans(selectedData(), input$clusters)
                       })

  output$plot1 <- renderPlot({
                               palette(c("#E41A1C", "#377EB8", "#4DAF4A", "#984EA3",
                                         "#FF7F00", "#FFFF33", "#A65628", "#F781BF", "#999999"))

                               par(mar = c(5.1, 4.1, 0, 1))
                               plot(selectedData(),
                                    col = clusters()$cluster,
                                    pch = 20, cex = 3)
                               points(clusters()$centers, pch = 4, cex = 4, lwd = 4)
                             })

}


end_time <- proc.time()

print((end_time - start_time))


# Return a Shiny app object
shinyApp(ui = ui, server = server)

