import { prefUtil } from '../common/prefUtil'
import BackComponent from '../component/BackComponent'
import { getApplyForEmailcode, postLogin, postRegister,  } from '../http/api/user'


@Entry
@Component
struct Register {
  @State username:string=''
  @State account:string =''
  @State password:string = ''
  @State repeatPassword:string=''
  @State email:string=''
  @State code:string=''

  @State codeCountdown:number=0
  @State showCodeInput:boolean=false

  uiContext:UIContext=this.getUIContext()

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
              Text('注册新玩家').font({size:26,weight:FontWeight.Bold}).fontColor(Color.Red)
              Text('开启您的游戏之旅').font({size:12})

            TextInput({text:this.username,placeholder:'请输入用户名'})
              .onChange((value)=>{
                this.username=value
              })
            TextInput({text:this.account,placeholder:'请输入账号'})
              .onChange((value)=>{
                this.account=value
              })
            TextInput({text:this.password,placeholder:'请输入密码'}).onChange((value)=>{
              this.password=value
            }).type(InputType.Password)
            TextInput({text:this.repeatPassword,placeholder:'请确认密码'}).onChange((value)=>{
              this.repeatPassword=value
            }).type(InputType.Password)
            Row(){
              TextInput({text:this.email,placeholder:'请输入邮箱（可选）'}).onChange((value)=>{
                if(value=='')
                  this.showCodeInput=false
                this.email=value
              }).width('70%')
              Button(this.codeCountdown==0?'获取验证码':this.codeCountdown.toString()).fontSize(12).backgroundColor('#ffd4d4d4').fontColor('#ff5d5c5c')
                .onClick(()=>{
                  if (!RegExp('^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$').test(this.email)){
                    this.showToast('邮箱输入错误')
                    return
                  }
                  getApplyForEmailcode(this.email).then((data)=>{
                    if(data.code==1){
                      this.showCodeInput=true
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

            if(this.showCodeInput){
              TextInput({text:this.code,placeholder:'请确认验证码'}).onChange((value)=>{
                this.code=value
              })
            }

            Button('立即注册').width("100%").type(ButtonType.Normal).borderRadius(5).backgroundColor(0xdc4639)
              .onClick(async ()=>{
                if (this.account==''||this.password==''||this.username==''){
                  this.showToast('用户名或账号或密码为空',Color.Red)
                  return
                }
                if(this.password!=this.repeatPassword){
                  this.showToast('两次密码不相同',Color.Red)
                  return
                }
                if(this.showCodeInput)
                  if(this.code==''){
                    this.showToast('验证码为空',Color.Red)
                    return
                  }
                // PostRegister
                await postRegister(this.username,this.account,this.password,this.email,this.code).then((data)=>{
                  if(data.code==1){
                    this.showToast('注册成功')
                    prefUtil.put('app','token',data.data.token)
                    this.uiContext.getRouter().clear()
                    this.uiContext.getRouter().replaceUrl({
                      url:'pages/Index'
                    })
                  }else {
                    this.showToast("注册失败: "+data.msg,Color.Red)
                  }
                }).catch((e:Error)=>{
                  console.error('postRegister Tag'+JSON.stringify(e))
                })
              })
          }.width(300).height(500).justifyContent(FlexAlign.SpaceEvenly).padding(10)
        }.height('100%').scrollBar(BarState.On)
      }
    }.height('100%').width('100%')
  }
}

@Extend(Button)
function buttonExtend(){
  .height(20)
  .fontSize(12)
  .buttonStyle(ButtonStyleMode.TEXTUAL)
}