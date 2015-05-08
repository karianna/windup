<!DOCTYPE html>

<#assign applicationReportIndexModel = reportModel.applicationReportIndexModel>

<html lang="en">
  <head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <title>${reportModel.projectModel.name} - JPA Report</title>
    <link href="resources/css/bootstrap.min.css" rel="stylesheet">
    <link href="resources/css/windup.css" rel="stylesheet" media="screen">
  </head>
  <body role="document">
	
	<!-- Navbar -->
	<div class="navbar navbar-default navbar-fixed-top">
		<div class="navbar-header">
			<button type="button" class="navbar-toggle" data-toggle="collapse" data-target=".navbar-responsive-collapse">
				<span class="icon-bar"></span>
				<span class="icon-bar"></span>
				<span class="icon-bar"></span>
			</button>
		</div>
		<div class="navbar-collapse collapse navbar-responsive-collapse">
			<ul class="nav navbar-nav">
			<#include "include/navbar.ftl">
			</ul>
		</div><!-- /.nav-collapse -->
	</div>
	<!-- / Navbar -->
	
	<div class="container-fluid" role="main">
		<div class="row">
			<div class="page-header page-header-no-border">
				<h1>JPA Report <span class="slash">/</span><small style="margin-left: 20px; font-weight: 100;">${reportModel.projectModel.name}</small></h1>
			</div>
		</div>
	
		<div class="row">
			<!-- Breadcrumbs -->
			<div class="container-fluid">
				<ol class="breadcrumb top-menu">
					<li><a href="../index.html">All Applications</a></li>
					<#include "include/breadcrumbs.ftl">
				</ol> 
			</div>
			<!-- / Breadcrumbs -->
		</div>

		<div class="row">
    		<div class="container-fluid theme-showcase" role="main">
		
		    <#if !reportModel.relatedResources.jpaConfiguration.list.iterator()?has_content>
		        <div class="panel panel-primary">
		            <div class="panel-heading">
		                <h3 class="panel-title">JPA Configurations</h3>
		            </div>
		            <div class="panel-body">
		                No JPA configuration files to report!
		            </div>
		        </div>
		    </#if>

		    <#list reportModel.relatedResources.jpaConfiguration.list.iterator() as jpaConfiguration>
			    <#list jpaConfiguration.persistenceUnits.iterator() as persistenceUnit>
			    <div class="panel panel-primary">
		            <div class="panel-heading">
		                <h3 class="panel-title">Persistence Unit: ${persistenceUnit.name}</h3>
		            </div>
		            <div class="panel-body">
						<dl class="dl-horizontal small">
							<dt>JPA Configuration</dt>
							<dd>${jpaConfiguration.prettyPath}</dd>
							
							<#if jpaConfiguration.specificationVersion??>
								<dt>JPA Version</dt>
								<dd>${jpaConfiguration.specificationVersion}</dd>
							</#if>
							
							<#if persistenceUnit.dataSource??>
								<#if persistenceUnit.dataSource.jndiLocation??>
									<dt>Data Source</dt>
									<dd>${persistenceUnit.dataSource.jndiLocation}</dd>
								</#if>
								
								<#if persistenceUnit.dataSource.databaseTypeName??>
									<dt>Data Source Type</dt>
									<dd>${persistenceUnit.dataSource.databaseTypeName}</dd>
								</#if>
							</#if>
						</dl>
						<#if persistenceUnit.properties?has_content>
			                <table class="table table-striped table-bordered" id="persistenceUnitPropertiesTable">
		                        <tr>
		                            <th>Persistence Unit Property</th><th>Value</th>
		                        </tr>
		                        <#list persistenceUnit.properties?keys as propKey>
		                            <tr>
		                                <td>${propKey}</td>
		                                <td>${persistenceUnit.properties[propKey]}</td>
		                            </tr>
		                        </#list>
				            </table>
			            </#if>
		            </div>
		        </div>
		        </#list>
		    </#list>

			<#if !reportModel.relatedResources.jpaEntities.list.iterator()?has_content>
		        <div class="panel panel-primary">
		        	<div class="panel-heading">
		                <h3 class="panel-title">JPA Entities</h3>
		            </div>
		            <div class="panel-body">
		                No JPA entity mapping files found to report!
		            </div>
		        </div>
		    </#if>

		    <#if reportModel.relatedResources.jpaEntities.list.iterator()?has_content>
		        <div class="panel panel-primary">
		            <div class="panel-heading">
		                <h3 class="panel-title">JPA Entities</h3>
		            </div>
    		        <table class="table table-striped table-bordered" id="jpaEntityTable">
		                <tr>
		                    <th>JPA Entity</th><th>Table</th>
		                </tr>
		                <#list reportModel.relatedResources.jpaEntities.list.iterator() as entity>
		          	        <tr>
		          		        <td>
		          			        <#if entity.javaClass??>
								        ${entity.javaClass.qualifiedName}
							        </#if>
						        </td>
		          		        <td>${entity.tableName!""}</td>
		          	        </tr>
		                </#list>
		            </table>
		        </div>
		    </#if>
	    </div> <!-- /container -->
	</div><!--/row-->
	
	</div><!-- /container main-->

    <script src="resources/js/jquery-1.10.1.min.js"></script>
    <script src="resources/js/bootstrap.min.js"></script>
  </body>
</html>