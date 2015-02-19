# copywiz
A simple copy wizard for improved file copy of multiple components.

# Build dependencies
You need the following libraries to compile and execute copywiz:

* yamlbeans 1.08 (https://github.com/EsotericSoftware/yamlbeans)
* Apache Commons IO 2.4 (http://commons.apache.org/proper/commons-io/)
* riverlayout (http://www.datadosen.se/riverlayout/)

# Configuration
This tool works by using YAML files for each component to copy. This is a template for the component files:

```
name: <name of the component>
installBasePath: <directory to fetch files from>
target: <directory to copy components to>
includedFiles:
  - <regex for included filenames>
  - <regex for included filenames>
excludedFiles:
  - <regex for excluded filenames>
  - <regex for excluded filenames>
runCommands:
  - <command to run>
  - <another command to run>
```
For example this may look like this:
```
name: Component A
installBasePath: /home/lycis/components/A
target: /home/lycis/insall/compA
includedFiles:
- .*
excludedFiles:
- *\.conf
- *.\xml
runCommands:
  - sh ./setup.sh
```
Config files must end with ```.yml``` and are searched for in the working folder of the application. When they are nested in 
some folders this structure will be represented in the UI.