setwd('C:/Users/redwa_000/Downloads/Max/0 UVa/Text Mining/Machine Problem 1')
data <- read.table("results.csv", header=TRUE, sep=",")
colnames(data)<-c("Token","Rank","TTF")

attach(data)

result<-lm(log(TTF) ~ log(Rank))
summary(result)

plot(log(Rank),log(TTF))
abline(result, col="purple")

plot(result$fitted.values,result$residuals)
abline(h=0,col="blue")

acf(result$residuals, main="ACF of Residuals")

qqnorm(result$residuals, ylab="Residuals", main="Normal Probability Plot of Residuals: Height Data")
qqline(result$residuals, col="green")

detach(data)
