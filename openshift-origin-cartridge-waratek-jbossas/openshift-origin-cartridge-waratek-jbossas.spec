%global cartridgedir %{_libexecdir}/openshift/cartridges/v2/jbossas
%global frameworkdir %{_libexecdir}/openshift/cartridges/v2/jbossas
%global jbossver 7.1.0.Final
%global oldjbossver 7.0.2.Final

Summary:       Provides JBossAS7 support
Name:          openshift-origin-cartridge-jbossas
Version: 1.1.1
Release:       1%{?dist}
Group:         Development/Languages
License:       ASL 2.0
URL:           http://www.openshift.com
Source0:       http://mirror.openshift.com/pub/openshift-origin/source/%{name}/%{name}-%{version}.tar.gz
Requires:      rubygem(openshift-origin-node)
Requires:      openshift-origin-node-util
Requires:      lsof
Requires:      java-1.7.0-openjdk
Requires:      java-1.7.0-openjdk-devel
Requires:      jboss-as7-modules >= %{jbossver}
Requires:	   facter
%if 0%{?rhel}
Requires:      jboss-as7 >= %{jbossver}
Requires:      maven3
%endif
%if 0%{?fedora}
Requires:      jboss-as
Requires:      bc
Requires:      maven
%endif
BuildRequires: git
BuildRequires: jpackage-utils
BuildArch:     noarch

%description
Provides JBossAS support to OpenShift. (Cartridge Format V2)


%prep
%setup -q


%build


%install
mkdir -p %{buildroot}%{cartridgedir}
mkdir -p %{buildroot}/%{_sysconfdir}/openshift/cartridges
cp -r * %{buildroot}%{cartridgedir}/

%post
%{_sbindir}/oo-admin-cartridge --action install --offline --source /usr/libexec/openshift/cartridges/v2/jbossas

%if 0%{?rhel}
alternatives --install /etc/alternatives/maven-3.0 maven-3.0 /usr/share/java/apache-maven-3.0.3 100
alternatives --set maven-3.0 /usr/share/java/apache-maven-3.0.3

alternatives --remove jbossas-7 /opt/jboss-as-7.1.0.Final
alternatives --install /etc/alternatives/jbossas-7 jbossas-7 /opt/jboss-as-7.1.0.Final 102
alternatives --set jbossas-7 /opt/jboss-as-7.1.0.Final
%endif

%if 0%{?fedora}
alternatives --remove maven-3.0 /usr/share/java/apache-maven-3.0.3
alternatives --install /etc/alternatives/maven-3.0 maven-3.0 /usr/share/maven 102
alternatives --set maven-3.0 /usr/share/maven

alternatives --remove jbossas-7 /usr/share/jbossas
alternatives --install /etc/alternatives/jbossas-7 jbossas-7 /usr/share/jbossas 102
alternatives --set jbossas-7 /usr/share/jbossas
%endif

#
# Temp placeholder to add a postgresql datastore -- keep this until the
# the postgresql module is added to jboss as7.* upstream.
mkdir -p /etc/alternatives/jbossas-7/modules/org/postgresql/jdbc/main
ln -fs /usr/share/java/postgresql-jdbc3.jar /etc/alternatives/jbossas-7/modules/org/postgresql/jdbc/main
cp -p %{cartridgedir}/versions/7/modules/postgresql_module.xml /etc/alternatives/jbossas-7/modules/org/postgresql/jdbc/main/module.xml

%{_sbindir}/oo-admin-cartridge --action install --offline --source /usr/libexec/openshift/cartridges/v2/jbossas


%files
%defattr(-,root,root,-)
%dir %{cartridgedir}
%dir %{cartridgedir}/bin
%dir %{cartridgedir}/metadata
%dir %{cartridgedir}/versions
%attr(0755,-,-) %{frameworkdir}
%{cartridgedir}/metadata/manifest.yml
%doc %{cartridgedir}/README.md
%doc %{cartridgedir}/COPYRIGHT
%doc %{cartridgedir}/LICENSE


%changelog
* Thu Apr 25 2013 Adam Miller <admiller@redhat.com> 1.1.1-1
- Bug 956497: Fix port bindings for jboss carts (ironcladlou@gmail.com)
- Card online_runtime_266 - Build PATH from
  CARTRIDGE_<CARTRIDGE_SHORT_NAME>_PATH (jhonce@redhat.com)
- cleanup (bdecoste@gmail.com)
- Split v2 configure into configure/post-configure (ironcladlou@gmail.com)
- more install/post-install scripts (dmcphers@redhat.com)
- Merge pull request #2193 from danmcp/master
  (dmcphers+openshiftbot@redhat.com)
- Merge pull request #2191 from jwhonce/wip/raw_envvar
  (dmcphers+openshiftbot@redhat.com)
- implementing install and post-install (dmcphers@redhat.com)
- Merge pull request #2188 from ironcladlou/dev/v2carts/hot-deploy
  (dmcphers+openshiftbot@redhat.com)
- Bug 954283 - JBoss standalone.conf sourcing env vars (jhonce@redhat.com)
- Implement hot deployment for V2 cartridges (ironcladlou@gmail.com)
- Bug 954283 (bdecoste@gmail.com)
- fix spec (bdecoste@gmail.com)
- Update outdated links in 'cartridges' directory. (asari.ruby@gmail.com)
- Merge pull request #2170 from bdecoste/master
  (dmcphers+openshiftbot@redhat.com)
- Bug 952051 (bdecoste@gmail.com)
- WIP Cartridge Refactor - Change environment variable files to contain just
  value (jhonce@redhat.com)
- Adding V2 Format to all v2 cartridges (calfonso@redhat.com)
- Bug 952513 (bdecoste@gmail.com)
- Merge pull request #2094 from BanzaiMan/dev/hasari/bz928675
  (dmcphers@redhat.com)
- Bug 928675 (asari.ruby@gmail.com)
- Add support for specifying websocket port in the manifest.
  (mrunalp@gmail.com)
- Bug 953041: Fix jbossas7 thread dump output to client (ironcladlou@gmail.com)
- Bug 952044 and 952043: JBoss v2 cart tidy fixes (ironcladlou@gmail.com)
- V2 documentation refactoring (ironcladlou@gmail.com)
- V2 cartridge documentation updates (ironcladlou@gmail.com)
- bump_minor_versions for sprint 2.0.26 (tdawson@redhat.com)

* Tue Apr 16 2013 Troy Dawson <tdawson@redhat.com> 1.0.7-1
- Setting mongodb connection hooks to use the generic nosqldb name
  (calfonso@redhat.com)

* Mon Apr 15 2013 Adam Miller <admiller@redhat.com> 1.0.6-1
- V2 action hook cleanup (ironcladlou@gmail.com)
- WIP Cartridge Refactor - Move PATH to /etc/openshift/env (jhonce@redhat.com)
- Merge pull request #2056 from bdecoste/master (dmcphers@redhat.com)
- fix version in spec (bdecoste@gmail.com)
- Adding connection hook for mongodb There are three leading params we don't
  care about, so the hooks are using shift to discard. (calfonso@redhat.com)

* Fri Apr 12 2013 Adam Miller <admiller@redhat.com> 1.0.5-1
- SELinux, ApplicationContainer and UnixUser model changes to support oo-admin-
  ctl-gears operating on v1 and v2 cartridges. (rmillner@redhat.com)
- be able to remove .openshift (bdecoste@gmail.com)
- install cart from spec (bdecoste@gmail.com)

* Thu Apr 11 2013 Dan McPherson <dmcphers@redhat.com> 1.0.4-1
- 

* Thu Apr 11 2013 Dan McPherson <dmcphers@redhat.com> 1.0.3-1
- new package built with tito

* Wed Apr 10 2013 Bill DeCoste <bdecoste@gmail.com> 1.0.2-1
- 

* Wed Apr 10 2013 Bill DeCoste <bdecoste@gmail.com> 1.0.1-1
- new package built with tito


