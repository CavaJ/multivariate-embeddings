# Title     : TODO
# Objective : TODO
# Created by: RBABAYEV
# Created on: 6/16/2020

# Load packages
library(shiny)
library(shinythemes)
library(dplyr)
library(readr)
library(ggplot2)

potasyum_data <- data.frame(date=seq(as.Date("2019/6/1"), by = "day", length.out = 30),
                            value_name = rep("Potasyum", 30),
                            result=sample(1:100, 30))

protein_data <- data.frame(date=seq(as.Date("2019/6/1"), by = "day", length.out = 30),
                           value_name = rep("Protein", 30),
                           result=sample(1:100, 30))

stack_data <- rbind(potasyum_data, protein_data)

ui <- fluidPage(
  titlePanel(h2("Health Monitoring Data",align = "center")),
  sidebarLayout(
    sidebarPanel(

      selectInput(inputId = "imputation_method",
                  label = "Choose an imputation method:",
                  choices = c("Mean", "Forward"),
                  selected = "Mean"),

      selectInput(inputId = "dataset",
                  label = "Choose a variable:",
                  choices = c("Potasyum", "Protein"),
                  selected = "Protein")),
    mainPanel(
      plotOutput("ts_plot"),
      verbatimTextOutput("summary"))))

server <- shinyServer(

  function(input,output){

    datasetInput <- reactive({
                               stack_data %>% filter(value_name == input$dataset)
                             })


    # Generate a summary of the dataset ----
    output$summary <- renderPrint({
                                    dataset <- datasetInput()
                                    summary(dataset$result)
                                  })

    # plot time series
    output$ts_plot <- renderPlot({

                                   dataset <- datasetInput()
                                   ggplot(dataset, aes(x = date, y=result)) + geom_line()

                                 })
  })



shinyApp(ui = ui, server = server)