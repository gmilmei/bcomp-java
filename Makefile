PREFIX=/usr
DESTDIR=
BINDIR=${PREFIX}/bin
BCOMPDIR=${PREFIX}/share/bcomp
JAR=target/compiler-0.9.jar
NAME=bcomp-java-0.9

build:
	mvn package
	java -cp ${JAR} gemi.bcomp.assembler.Assembler libb/libb.bs -o libb/libb.bo

install:
	sed "s|@BCOMPDIR@|${BCOMPDIR}|g" scripts/b-as.in > scripts/b-as
	sed "s|@BCOMPDIR@|${BCOMPDIR}|g" scripts/b-comp.in > scripts/b-comp
	sed "s|@BCOMPDIR@|${BCOMPDIR}|g" scripts/b-dis.in > scripts/b-dis
	sed "s|@BCOMPDIR@|${BCOMPDIR}|g" scripts/b-link.in > scripts/b-link
	sed "s|@BCOMPDIR@|${BCOMPDIR}|g" scripts/b-vm.in > scripts/b-vm
	mkdir -p ${DESTDIR}${BINDIR}
	mkdir -p ${DESTDIR}${BCOMPDIR}
	install -m0755 scripts/b-as ${DESTDIR}${BINDIR}
	install -m0755 scripts/b-comp ${DESTDIR}${BINDIR}
	install -m0755 scripts/b-dis ${DESTDIR}${BINDIR}
	install -m0755 scripts/b-link ${DESTDIR}${BINDIR}
	install -m0755 scripts/b-vm ${DESTDIR}${BINDIR}
	install -m0644 ${JAR} ${DESTDIR}${BCOMPDIR}/bcomp.jar
	install -m0644 libb/libb.bo ${DESTDIR}${BCOMPDIR}/libb.bo

tar: clean
	mkdir -p ${NAME}
	cp -a COPYING INSTALL.txt README.md ${NAME}
	cp -a Makefile pom.xml ${NAME}
	cp -a doc libb scripts src ${NAME}
	tar zcf ${NAME}.tar.gz --owner=0 --group=0 ${NAME}
	rm -rf ${NAME}

clean:
	mvn clean
	rm -f libb/libb.bo
	rm -f scripts/b-as
	rm -f scripts/b-comp
	rm -f scripts/b-dis
	rm -f scripts/b-link
	rm -f scripts/b-vm
	rm -f ${NAME}.tar.gz
