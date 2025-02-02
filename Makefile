all: build

build:
	./"gradlew" createDistributable

distribution:
	./"gradlew" packageDistributionForCurrentOS