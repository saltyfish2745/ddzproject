import BackComponent from '../component/BackComponent'
import { getApplyForEmailcode, postRetrieve } from '../http/api/user'

@Entry
@Component
struct Retrieve {
  uiContext = this.getUIContext()

  @State account:string = ''
  @State password:string = ''
  @State repeatPassword:string = ''
  @State email:string = ''
  @State code:string = ''

  @State codeCountdown:number=0
  private showToast(mes:string,color?:ResourceColor){
    this.uiContext.getPromptAction().showToast({
      message:mes,
      textColor:color
    })
  }

  build() {
    Stack(){
      BackComponent(){
        Scroll(){
          Column({space:10}){
            Text('找回密码').font({size:26,weight:FontWeight.Bold}).fontColor(Color.Red)
            Text('重置您的登录密码').font({size:12})

            TextInput({text:$$this.account,placeholder:'请输入账号'})
            TextInput({text:$$this.password,placeholder:'请输入新密码'}).type(InputType.Password)
            TextInput({text:$$this.repeatPassword,placeholder:'请确认新密码'}).type(InputType.Password)
            Row({space:3}){
              TextInput({text:$$this.email,placeholder:'请输入邮箱（可选）'}).width('70%')
              Button(this.codeCountdown==0?'获取验证码':this.codeCountdown.toString())
                .fontSize(10).type(ButtonType.Normal).border({radius:5})
                .backgroundColor('#ffd4d4d4').fontColor('#ff5d5c5c').width('30%')
                .onClick(()=>{
                  if (!RegExp('^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$').test(this.email)){
                    this.showToast('邮箱输入错误')
                    return
                  }

                  getApplyForEmailcode(this.email).then((data)=>{
                    if(data.code==1){
                      this.showToast('请求成功')
                      this.codeCountdown=data.data
                      let timer=setInterval(()=>{
                        this.codeCountdown--
                        if(this.codeCountdown==0){
                          clearInterval(timer)
                        }
                      },1000)
                    }else {
                      this.showToast('请求失败',Color.Red)
                    }
                  }).catch((e:Error)=>{
                    console.error('getApplyForEmailcode Tag'+JSON.stringify(e))
                  })
                })
            }.width('100%')

            TextInput({text:$$this.code,placeholder:'请确认验证码'})

            Button('重置密码').width("100%").type(ButtonType.Normal).borderRadius(5).backgroundColor(0xdc4639)
              .onClick(()=>{
                if (this.account==''||this.password==''){
                  this.showToast('用户名或账号或密码为空',Color.Red)
                  return
                }
                if(this.password!=this.repeatPassword){
                  this.showToast('两次密码不相同',Color.Red)
                  return
                }
                if(this.code==''){
                  this.showToast('验证码为空',Color.Red)
                  return
                }

                postRetrieve(this.account,this.password,this.email,this.code).then(res=>{
                  if(res.code==1){
                    this.showToast('重置成功')
                    this.uiContext.getRouter().back()
                  }else {
                    this.showToast('重置失败: '+res.msg,Color.Red)
                  }})
                  .catch((e:Error)=>{
                    console.error('postRegister Tag'+JSON.stringify(e))
                  })
              })
          }.width(320).height(450).justifyContent(FlexAlign.SpaceAround).padding(20)
        }.height('100%').scrollBar(BarState.On)
      }
    }.height('100%').width('100%')
  }
}