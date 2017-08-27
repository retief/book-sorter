# book-sorter

Tech demo for startup

## Usage

Install java 8 (check java -version), install lein, install phantomjs

Run lein figwheel in one terminal and lein run 3000 in another terminal.
The first sets up the frontend code reloading and the second runs the
backend server.

lein test runs the backend tests, and lein cljsbuild test runs the frontend
tests.  Both of these start very slowly, I think there is a way to have them
automatically run on file change.  Need to look into it -- lein doo might be
what we need.

## License

Copyright © 2017 William

Distributed under the Eclipse Public License version 1.0
