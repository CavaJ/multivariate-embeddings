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






ui <- bootstrapPage(

  selectInput(inputId = "n_breaks",
              label = "Number of bins in histogram:",
              choices = c(10, 20, 35, 50),
              selected = 20),

  checkboxInput(inputId = "individual_obs",
                label = strong("Show individual observations"),
                value = FALSE),

  checkboxInput(inputId = "density",
                label = strong("Show density estimate"),
                value = FALSE),

  plotOutput(outputId = "main_plot", height = "300px"),

  # Display this only if the density is shown
  conditionalPanel(condition = "input.density == true",
                   sliderInput(inputId = "bw_adjust",
                               label = "Bandwidth adjustment:",
                               min = 0.2, max = 2, value = 1, step = 0.2)
  )

)


server <- function(input, output) {

  output$main_plot <- renderPlot({

                                   hist(faithful$eruptions,
                                        probability = TRUE,
                                        breaks = as.numeric(input$n_breaks),
                                        xlab = "Duration (minutes)",
                                        main = "Average recording duration")

                                   if (input$individual_obs) {
                                     rug(faithful$eruptions)
                                   }

                                   if (input$density) {
                                     dens <- density(faithful$eruptions,
                                                     adjust = input$bw_adjust)
                                     lines(dens, col = "blue")
                                   }

                                 })
}


# Return a Shiny app object
shinyApp(ui = ui, server = server)