<template>
  <div id="bg">
    <div id="login-model">
      <div id="banner">
        <div> <div id="logo">
            <svg class="image-svg-svg primary" height="100" style="overflow: visible;" width="100"
                 x="0" y="0">
                <svg viewBox="0 0 86.52999877929688 100" xmlns="http://www.w3.org/2000/svg">
                    <path d="M20.82 87.16L0 75.28V25.04l20.82 12.33v49.79z" fill="#2084f7"></path>
                    <path d="M18.95 14.03L0 25.04l20.82 12.33 18.95-10.83-20.82-12.51z" fill="#22cfee"></path>
                    <path d="M86.53 50.55l-.11 24.39L43.03 100l.14-24.56 43.36-24.89z" fill="#23b0f1"></path>
                    <path d="M24.1 88.87L43.03 100l.14-24.56-19.02-10.58-.05 24.01z" fill="#2084f7"></path>
                    <path d="M22.41 12.17L43.06 0l43.28 25.04L65.92 37.2 22.41 12.17z" fill="#22cfee"></path>
                    <path d="M86.37 46.98l-.03-21.94L65.92 37.2l.02 21.81 20.43-12.03z" fill="#23b0f1"></path>
                    <path d="M60.79 60.12L43.27 70.23 25.75 60.12V39.88l17.52-10.11 17.52 10.11v20.24z"
                          fill="#22cfee"></path>
                </svg>
            </svg>
            <svg height="100"  style="margin-left: 5px;" viewBox="0 0 168 42"  width="100" xmlns="http://www.w3.org/2000/svg">
                <path
                    d="M23.52-5.46L29.1-5.46 41.16-26.88 41.46-26.88 41.46 0 49.56 0 49.56-42 41.34-42 26.82-15.18 26.58-15.18 12.18-42 4.08-42ZM4.08 0L11.94 0 11.94-16.5 4.08-31.26ZM65.82-35.52L65.82-42 57.66-42 57.66-35.52ZM57.66 0L65.76 0 65.76-30 57.66-30ZM102.36 0L110.28 0 110.28-42 102.54-42 102.54-13.08 102.24-13.08 81.96-42 73.62-42ZM73.98 0L81.84 0 81.84-20.58 73.98-31.8ZM126.54-35.52L126.54-42 118.38-42 118.38-35.52ZM118.38 0L126.48 0 126.48-30 118.38-30ZM146.52-18.12L164.34-18.12 164.34 0 172.44 0 172.44-42 164.34-42 164.34-25.8 146.52-25.8ZM134.7 0L142.8 0 142.8-42 134.7-42Z"
                    fill="#ffffff"
                    transform="translate(-4.079999923706055, 42)"></path>
            </svg>
        </div>
           <h2 style="margin-top: 50px"> welcome back :)</h2>
        </div>
      </div>
      <div id="login">
        <div id="welcome">
          <div id="text-content">
            <h2>你好！</h2>
            <h2>欢迎使用！</h2>
          </div>
        </div>
        <div id="login-form">
          <basic-form></basic-form>

        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import BasicForm from "./form/BasicForm.vue";

</script>

<style scoped>

#bg {
  width: 100vw;
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background-image: var(--background-color);

  #login-model {
    width: var(--model-with);
    height: calc(var(--model-with) / 2);
    background-color: rgba(0, 0, 0, 0.2);
    border-radius: 15px;
    overflow: hidden;
    display: flex;

    #banner {
      width: calc(var(--model-with) / 2);
      flex: 1;
      color: #FFF;
      display: flex;
      justify-content: center;
      align-items: center;
    }

    #login {
      box-sizing: border-box;
      padding: var(--login-model-padding);
      width: calc(var(--model-with) / 2);
      background-color: #FFF;
      height: 100%;
      flex-direction: column;

      #welcome {
        flex-direction: column;

        #text-content {
          flex-direction: column;
          text-align: left;
          align-items: center;

          * {
            display: block;
            flex: none;
          }
        }
      }

      #login-form {
        margin-top: 30px;
      }
    }


  }
}


</style>