all: build

build:
	./gradlew createDistributable

distribute:
	./gradlew packageDistributionForCurrentOS