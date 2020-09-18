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


# Global variables can go here
n <- 200


# Define the UI
ui <- bootstrapPage(
  numericInput('n', 'Number of obs', n),
  plotOutput('plot')
)


# Define the server code
server <- function(input, output) {
  output$plot <- renderPlot({
                              hist(runif(input$n))
                            })
}

# Return a Shiny app object
shinyApp(ui = ui, server = server)