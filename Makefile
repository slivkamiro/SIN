install:
	chmod +x INSTALL
	./INSTALL

compile:
	chmod +x COMPILE
	./COMPILE

run:
	chmod +x RUN
	./RUN

zip:
	zip 1-xslivk02-xstari01-50-50.zip -r src/ INSTALL COMPILE RUN Makefile

clean:
	rm -rf build/ bin/