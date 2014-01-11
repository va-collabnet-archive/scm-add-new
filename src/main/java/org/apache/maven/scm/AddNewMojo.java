package org.apache.maven.scm;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.scm.command.add.AddScmResult;
import org.apache.maven.scm.command.status.StatusScmResult;
import org.apache.maven.scm.log.DefaultLog;
import org.apache.maven.scm.plugin.AddMojo;
import org.apache.maven.scm.provider.svn.svnexe.command.status.SvnStatusCommandFixed;
import org.apache.maven.scm.repository.ScmRepository;

/**
 * Goal which does a scm add, but only for newly added resources.  Note, this is a hack.
 * It will only work for folders at the moment with SVN, and it doesn't honor includes and excludes filters - 
 * It will fail if one is provided.
 * 
 * For whatever root directory is provided, it will add every file that is not yet included in the repo.
 * 
 * 
 * @extendsPlugin maven-scm-plugin
 * @extendsGoal add
 * @goal add-new
 * 
 * @phase process-sources
 */
public class AddNewMojo extends AddMojo
{

	public void execute() throws MojoExecutionException
	{
		if (!StringUtils.isEmpty(getIncludes()) || !StringUtils.isEmpty(getExcludes()))
		{
			throw new MojoExecutionException("Sorry, this add-new-hack doesn't support includes and excludes filters");
		}
		try
		{
			ScmRepository repository = getScmRepository();
			boolean firstPass = true;
			while (true)
			{
				//the maven scm stuff ignores folders... it doesn't even tell you they are not there.
				//Hack fix for SVN, won't work for other providers... (will only add new files for those, ignores folders)
				StatusScmResult status;
				if (getConnectionUrl().toLowerCase().startsWith("scm:svn"))
				{
					SvnStatusCommandFixed sscf = new SvnStatusCommandFixed();
					sscf.setLogger(new DefaultLog(getLog().isDebugEnabled()));
					status = sscf.executeStatusCommand(repository.getProviderRepository(), getFileSet());
				}
				else
				{
					status = getScmManager().status(repository, getFileSet());
				}
	
				List<ScmFile> changedFiles = status.getChangedFiles();
	
				ArrayList<File> addedFiles = new ArrayList<File>();
				for (ScmFile f : changedFiles)
				{
					if (f.getStatus() == ScmFileStatus.UNKNOWN)
					{
						addedFiles.add(new File(f.getPath()));
						getLog().info(" New File " + f.getPath());
					}
				}
	
				if (addedFiles.size() > 0)
				{
				
					ScmFileSet fs = new ScmFileSet(getWorkingDirectory(), addedFiles);
		
					AddScmResult result = getScmManager().add(repository, fs);
		
					checkResult(result);
		
					getLog().info("" + result.getAddedFiles().size() + " files successfully added.");
				}
				else
				{
					if (firstPass)
					{
						getLog().info("No new files were found that needed adding");
					}
					break;
				}
				firstPass = false;
			}
		}
		catch (ScmException e)
		{
			throw new MojoExecutionException("Cannot run add command : ", e);
		}
		catch (IOException e)
		{
			throw new MojoExecutionException("Cannot run add command : ", e);
		}
	}
}