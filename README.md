[![Artifact](https://maven-badges.herokuapp.com/maven-central/eu.lundegaard.liferay/liferay-db-setup-core/badge.svg?color=blue)](https://search.maven.org/search?q=g:eu.lundegaard.liferay%20AND%20a:liferay-db-setup-core) [![Javadocs](https://www.javadoc.io/badge/eu.lundegaard.liferay/liferay-db-setup-core.svg?color=blue)](https://www.javadoc.io/doc/eu.lundegaard.liferay/liferay-db-setup-core)

# Liferay Portal DB Setup core
Library that allows to setup a number of Liferay artifacts in a DB. It uses xml configuration and Liferay APIs to add all configured artifacts. Artifacts in the database are created by Liferay common **upgrade process**. Each step of the upgrade process consists of one or more xml files, in which you can define artifacts to create or update.

## Usage

First add this dependency into your OSGi module project's `pom.xml`.

```xml
<dependency>
    <groupId>eu.lundegaard.liferay</groupId>
    <artifactId>liferay-db-setup-core</artifactId>
    <version>3.0.0</version>
</dependency>
```

and specify the dependency in your `bnd.bnd` file as a resource to include.

```properties
Include-Resource: @liferay-db-setup-core-3.0.0.jar
```

Second create `UpgradeStepRegistrator` component to register your upgrade steps, e.g.

```java
@Component(immediate = true, service = UpgradeStepRegistrator.class)
public class MyPortalUpgrade implements UpgradeStepRegistrator {

    @Override
    public void register(Registry registry) {
        String packageName = MyPortalUpgrade.class.getPackage().getName();
        registry.register(packageName, "1.0.0", "1.0.1", new GenericUpgradeStep("v1_0_1"));
    }
    
}
```

You can also call one of the `LiferaySetup.setup` methods directly to setup the database.

### XML File content

XML file of an upgrade step has usually this structure:

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<setup xmlns="http://www.lundegaard.eu/liferay/setup">
    <configuration>
        <runasuser>test@liferay.com</runasuser>
    </configuration>

    <!-- Artifacts to manage --> 
</setup>
```

`runasuser` defines under which user artifacts will be created. Then you can specify as many artifacts to setup as you want.

For instance, this will create **Role** with Publisher as a name.

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<setup xmlns="http://www.lundegaard.eu/liferay/setup">
    <configuration>
        <runasuser>test@liferay.com</runasuser>
    </configuration>
 
    <roles>
        <role name="Publisher"/>
    </roles>
</setup>
```

## Features

In **Artifacts to manage** section you can specify a lot of artifacts.

### Role

```xml
<roles>
    <role name="Publisher"/>
</roles>
```

### Expando attribute

This will create expando attribute **canonical-url** with permissions to view by guest user.

```xml
<customFields>
    <field name="canonical-url" type="string" className="com.liferay.portal.kernel.model.Layout">
        <role-permission role-name="Guest">
            <permission-action action-name="VIEW"/>
        </role-permission>
    </field>
</customFields>
```

### Site

Site element must always have `site-friendly-url` filled. Guest site is determined by `default` attribute with `true` value. Other sites are specified by `name` attribute.

```xml
<sites>
    <site default="true" site-friendly-url="/guest">
    </site>
    <site name="My web" default="false" >
    </site>
</sites>
```

### Document

Document's file itself is determined by `file-system-name` attribute which defines resource on classpath.

```xml
<document file-system-name="my-project/documents/icons/icon-home.svg"
          document-folder-name="/Icons"
          document-filename="icon-home.svg"
          document-title="icon-home.svg"/>
```

### Articles

Article's content is determined by `path` attribute which defines resource on classpath. The resource contains article content in the form of XML.

```xml
<article title="Footer"
         path="my-project/articles/homepage/web_content/footer.xml"
         article-structure-key="BASIC-WEB-CONTENT"
         article-template-key="BASIC-WEB-CONTENT"
         articleId="FOOTER"
         article-folder-path="/Footer">
</article>

<article-structure key="BANNER-MAIN"
                   path="my-project/articles/homepage/structures/banner-main.json"
                   name="Banner - main"/>

<article-template key="BANNER-MAIN"
                  path="my-project/articles/homepage/templates/banner-main.ftl"
                  article-structure-key="BANNER-MAIN" name="Banner - main" cacheable="true"/>
```

### Others

You can create/update/set many other artifacts like User, Organization, Page, Portlet placement, Permission, ... See source code.

## Compatibility
* Version 3.x.x: Liferay Portal 7.2.x

For older versions of portal use original [Mimacom library](https://github.com/mimacom/liferay-db-setup-core).
