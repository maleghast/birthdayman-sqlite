# BirthdayMan - a Toy App for managing birthday reminders

## Introduction

This is a "toy" app based on the app created by Dan Amber as part of an online video / tutorial about
[NBB](https://github.com/babashka/nbb) - here's a link to his [project](https://github.com/danownsthisspace/birthman)

In the spirit of trying to learn NBB for myself but get a flying start from Dan's excellent video, I decided
to change the DB to sqlite3 and to add another bit of data (what the person wants for their birthday), and
then add extra functionality to change the gift idea and to be able to search for birthdays on an arbitrary date.

[Here's a link to Dan's video](https://youtu.be/_-G9EKaAyuI) - The video is Dan coding and getting pair assistance
from the excellent [borkdude](https://github.com/borkdude), creator of Babashka and NBB amongst other awesome stuff
from the Clojure Ecosystem.

## Usage - Installation

If you just want to play around with this for yourself the simplest way to do that is to clone the repo, and then follow these steps:

1. cp birthdays.db.template birthdays.db
2. npm install nbb -g
3. npm install

(You will need a current version of NodeJS to satisfy the dependency needs of NBB)

At some point in the near future I will be packaging the app up for NPM.  When this
is done I will add instructions to simply install with npx.

## Usage - Running the app

Simply run the following in the root of the project to add a Birthday that you want to remember:

nbb app.cljs

If you want to see a list of commands available, use:

nbb app.cljs help