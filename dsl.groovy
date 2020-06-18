job("Code_Interpreter") {

  description("Code Interpreter Job")
  
  steps {
  
   scm {
     git {
       extensions {
         wipeWorkspace()
       }
     }
   }
    
   scm {
     github("Ninad07/Groovy", "master")
   }

   triggers {
     scm("* * * * *")
   }

   shell("sudo cp -rvf * /groovy/code/")

   if(shell("ls /groovy/code/ | grep php | wc -l")) {

     dockerBuilderPublisher {
       dockerFileDirectory("/groovy/image/html")
       fromRegistry {
         url("dockerninad07")
         credentialsId("4de5b343-12cc-4a68-9e1c-8c4d1ce40917")
       }
       cloud("Local")
    
       tagsString("dockerninad07/apache-server")
       pushOnSuccess(true)
       pushCredentialsId("8fb4a5df-3dab-4214-a8ec-7f541f675dcb")
       cleanImages(false)
       cleanupWithJenkinsJobDelete(false)
       noCache(false)
       pull(true)
     }    
      
   }
   
   else {
     
     dockerBuilderPublisher {
       dockerFileDirectory("/groovy/image/php")
       fromRegistry {
         url("dockerninad07")
         credentialsId("4de5b343-12cc-4a68-9e1c-8c4d1ce40917")
       }
       cloud("Local")

       tagsString("dockerninad07/apache-php-server")
       pushOnSuccess(true)
       pushCredentialsId("8fb4a5df-3dab-4214-a8ec-7f541f675dcb")
       cleanImages(false)
       cleanupWithJenkinsJobDelete(false)
       noCache(false)
       pull(true)
     } 
   }

  }
}

job("Kubernetes_Deployment") {

  description("Kubernetes job")
    
  triggers {
    upstream {
      upstreamProjects("Code_Interpreter")
      threshold("SUCCESS")
    }  
  }

  steps {
    if(shell("ls /groovy/code | grep php | wc -l")) {

      shell("if kubectl get deployments | grep html-dep; then kubectl rollout restart deployment/html-dep; kubectl rollout status deployment/html-dep; else kubectl create -f /groovy/dep/http_pv.yml; kubectl create -f /groovy/dep/http_pv_claim.yml; kubectl create -f /groovy/dep/http_dep.yml; kubectl create -f service.yml; kubectl get deployment html-dep; fi")       

  }

    else {

      shell("if kubectl get deployments | grep php-dep; then kubectl rollout restart deployment/php-dep; kubectl rollout status deployment/php-dep; else kubectl create -f /groovy/dep/php_pv.yml; kubectl create -f /groovy/dep/php_pv_claim.yml; kubectl create -f /groovy/dep/php_dep.yml; kubectl create -f service.yml; kubectl get deployment php-dep; fi")

    }
  }
}

job("Application_Monitoring") {
  
  description("Application Monitoring Job")

  triggers {
     scm("* * * * *")
   }

  steps {
    shell('export status=$(curl -siw "%{http_code}" -o /dev/null 192.168.99.106:30100); if [ $status -eq 200 ]; then exit 0; else python3 email.py; exit 1; fi')
  }
}

job("Redeployment") {

  description("Redeploying the Application")

  triggers {
    upstream {
      upstreamProjects("Application_Monitoring")
      threshold("FAILURE")
    }
  }
  
  
  publishers {
    postBuildScripts {
      steps {
        downstreamParameterized {
  	  	  trigger("Code_Interpreter")
        }
      }
    }
  }
}