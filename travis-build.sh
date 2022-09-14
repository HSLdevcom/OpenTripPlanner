#!/bin/bash
set -e

# This is run at ci, created an image that contains all the tools needed in
# databuild
#
# Set these environment variables
#DOCKER_USER // dockerhub credentials
#DOCKER_AUTH

ORG=${ORG:-hsldevcom}
DOCKER_TAG=${TRAVIS_COMMIT:-latest}
DOCKER_IMAGE=$ORG/opentripplanner
DOCKER_IMAGE_COMMIT=$DOCKER_IMAGE:$DOCKER_TAG
DOCKER_IMAGE_LATEST=$DOCKER_IMAGE:latest
DOCKER_IMAGE_PROD=$DOCKER_IMAGE:prod
DOCKER_IMAGE_OTP2=$DOCKER_IMAGE:otp2
DOCKER_IMAGE_OTP2_PROD=$DOCKER_IMAGE:otp2-prod

if [ -z $TRAVIS_TAG ]; then
  # Build image
  echo Building OTP
  docker build --tag="$ORG/$DOCKER_IMAGE:builder" -f Dockerfile.builder .
  mkdir export
  docker run --rm --entrypoint tar "$ORG/$DOCKER_IMAGE:builder" -c target|tar x -C ./
  #package OTP quietly while keeping travis happpy
  docker build --tag="$DOCKER_IMAGE_COMMIT" -f Dockerfile .
fi

if [ "${TRAVIS_PULL_REQUEST}" == "false" ]; then
  docker login -u $DOCKER_USER -p $DOCKER_AUTH
  if [ "$TRAVIS_TAG" ];then
    echo "processing release $TRAVIS_TAG"
    docker pull $DOCKER_IMAGE_COMMIT
    docker tag $DOCKER_IMAGE_COMMIT $DOCKER_IMAGE_PROD
    docker push $DOCKER_IMAGE_PROD
  else
    if [ "$TRAVIS_BRANCH" = "master" ]; then
      echo "Pushing latest image"
      docker push $DOCKER_IMAGE_COMMIT
      docker tag $DOCKER_IMAGE_COMMIT $DOCKER_IMAGE_LATEST
      docker push $DOCKER_IMAGE_LATEST
    elif [ "$TRAVIS_BRANCH" = "dev-2.x" ]; then
      echo "Pushing otp2 image"
      docker push $DOCKER_IMAGE_COMMIT
      docker tag $DOCKER_IMAGE_COMMIT $DOCKER_IMAGE_OTP2
      docker push $DOCKER_IMAGE_OTP2
    elif [ "$TRAVIS_BRANCH" = "otp2-prod" ]; then
      echo "Pushing otp2-prod image"
      docker push $DOCKER_IMAGE_COMMIT
      docker tag $DOCKER_IMAGE_COMMIT $DOCKER_IMAGE_OTP2_PROD
      docker push $DOCKER_IMAGE_OTP2_PROD
    fi
  fi
fi

echo Build completed