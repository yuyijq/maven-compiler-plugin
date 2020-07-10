通过修改maven-compiler-plugin，实现真.增量编译

maven原本的增量编译基本上是废物:
```
maven-compiler-plugin有个增量编译参数，打开之后的行为是如果这个模块里有一个源文件发生变动就会触发完整的编译(这是增量编译?)。如果关闭这个增量编译，这个时候的行为倒更像“增量编译”，但是这个时候有几个问题：1. 如果源文件被删除了，class是不会删除的。这个大部分时候没问题，但是和比如spring等的自动扫描不搭。2. 如果A引用了B类的一个常量，这个时候B类常量值变化了，A类没变，那么A是不会重新编译的，这当然不行。
```
修改后的插件，每次编译完成之后会使用asm对class进行解析，解析出类之间的依赖关系，然后将其持久化到磁盘中。当下次编译的时候会首先加载上一次编译生成的依赖关系文件，然后扫描源代码目录里的*.java文件，如果该文件有变动则将依赖类也读取出来，将本次修改的以及涉及的依赖都拿出来重新编译。
使用方法:

* 在pom里引入本插件
```
<build>
		<pluginManagement>
			<plugins>
				<plugin>
					<groupId>org.apache.maven.plugins</groupId>
					<artifactId>maven-compiler-plugin</artifactId>
					<version>3.9.0-SNAPSHOT</version>
					<configuration>
                        <!-- 关闭废物maven的增量编译 -->
						<useIncrementalCompilation>false</useIncrementalCompilation>
                        <!-- 打开增量编译 -->
						<wormpexIncrementCompile>true</wormpexIncrementCompile>
						<source>${java.source.version}</source>
						<target>${java.target.version}</target>
						<encoding>${file.encoding}</encoding>
						<debug>true</debug>
						<fork>true</fork>
					</configuration>
				</plugin>
			</plugins>
		</pluginManagement>
	</build>
```

* 每次编译的时候使用mvn package，不要clean